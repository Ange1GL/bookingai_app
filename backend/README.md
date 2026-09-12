# 🤖 Spring Boot AI Multi-Agent Engine

Un motor de flujos agénticos orquestados utilizando **Spring Boot 4**, **Java 25** y el ecosistema **Spring AI**. La aplicación implementa patrones de coordinación entre agentes autónomos (Orchestrator-Worker, Dynamic Routing y Human-in-the-Loop) con ejecución mediante *Virtual Threads* y un servidor MCP (*Model Context Protocol*) integrado.

---

## 🚀 Características Principales

* **Lógica Agéntica con Spring AI:** Definición de agentes especializados con capacidades de ejecución de herramientas (*Tool Calling*).
* **Construcción en Java 25:** Aprovechamiento de construcciones de sintaxis modernas como *Flexible Constructor Bodies* (JEP 513) y soporte optimizado para *Virtual Threads*.
* **Ecosistema Spring Boot 4:** Configuración nativa basada en Spring Framework 7 y Jakarta EE 11.
* **Servidor MCP Integrado:** Protocolo estandarizado de contexto para exponer y consumir *Tools* de forma remota.
* **Soporte Multi-Modelo:** Integración directa con proveedores como OpenAI, Ollama, Anthropic y DeepSeek.

---

## 🛠️ Requisitos Previos

* **JDK 25** (o posterior)
* **Maven 3.9+** o **Gradle 8.14+**
* Llave de API del proveedor configurado (ej. `OPENAI_API_KEY`)

---

## 📦 Instalación y Configuración

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/tu-usuario/spring-boot-ai-agents.git](https://github.com/tu-usuario/spring-boot-ai-agents.git)
   cd spring-boot-ai-agents