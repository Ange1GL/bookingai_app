# Known Issue: Azure OpenAI blocks "September 11" date references

**Detected:** 2026-09-10  
**Affected stack:** `spring-ai-starter-model-openai` connected to Azure AI Foundry (Microsoft Foundry) endpoints

---

## Description

When sending a prompt that includes the date "11 de septiembre" (or "September 11") to any Azure OpenAI deployment via the Foundry endpoint, Azure returns an HTTP 400 with the following message:

```
The response was filtered due to the prompt triggering Azure OpenAI's content management policy.
Please modify your prompt and retry.
```

This causes Spring AI to propagate a `400` exception that the application catches and returns as `500 Internal Server Error` to the client.

## Root cause

Azure OpenAI's default content filter policy (`Microsoft.DefaultV2`) runs a **Violence category classifier** on every prompt. The phrase "September 11" / "11 de septiembre" is associated with the 2001 terrorist attacks, and the classifier scores it at **medium or high severity**, which the default policy blocks.

This is a **false positive** — a legitimate date reference being caught by the violence classifier. It is not documented by Microsoft as intentional behavior.

## Reproduction

```http
POST /api/v1/agent/chat
Content-Type: application/json

{
  "sessionId": "any-session-id",
  "message": "Agenda una cita para Sonia el 11 de septiembre de 2026 de 3:00 p.m. a 3:30 p.m."
}
```

**Result:** HTTP 500 — Azure returns 400 content policy violation.  
**Other dates (Sep 13, 14, 16, 20, etc.):** Work without issue.

## Workaround

Create a custom content filter policy in Azure AI Foundry with the **Violence threshold raised from `Medium` to `High`**, then assign it to the active model deployment.

Steps:
1. Open [Azure AI Foundry portal](https://ai.azure.com)
2. Navigate to your Azure OpenAI resource → **Content Filters**
3. Create a new policy and set **Violence** threshold to **High**
4. Assign the policy to the deployment (e.g. `gpt-4.1`)

No Microsoft approval is required for threshold adjustments within the allowed range.

## Where to report

| Channel | Link |
|---|---|
| Azure Feedback forum | https://feedback.azure.com/d365community/forum/a28c4975-5f25-ec11-b6e6-000d3a4f07b8 |
| Azure Support ticket | portal.azure.com → Help + Support → New support request → "Azure OpenAI Service" |
| Spring AI GitHub Issues | https://github.com/spring-projects/spring-ai/issues |

Note: the filter runs on the Azure side — Spring AI and the application code are not at fault.
