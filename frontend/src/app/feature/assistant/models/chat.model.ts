/** Mirrors the backend ChatRequest record. */
export interface ChatRequestDto {
  sessionId: string;
  message: string;
}

/** Mirrors the backend ChatResponse record. */
export interface ChatResponseDto {
  reply: string;
}

/** Mirrors the backend SessionResponse record. */
export interface SessionResponseDto {
  sessionId: string;
}

export type ChatRole = 'user' | 'assistant';

/** `error` only applies to user messages whose delivery failed and can be retried. */
export type ChatMessageState = 'sent' | 'error';

export interface ChatMessage {
  id: number;
  role: ChatRole;
  text: string;
  state: ChatMessageState;
  createdAt: Date;
}

/** A ready-made prompt offered as a card (welcome) or a chip (above the composer). */
export interface QuickAction {
  label: string;
  description: string;
  icon: string;
  prompt: string;
}
