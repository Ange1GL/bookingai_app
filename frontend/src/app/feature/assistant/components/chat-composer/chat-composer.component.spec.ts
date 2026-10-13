import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ErrorHandlerService } from '@/core/service/error-handler.service';
import { AgentService } from '../../service/agent.service';
import { VoiceInputComponent } from '../voice-input/voice-input.component';
import { ChatComposerComponent, MESSAGE_MAX_LENGTH } from './chat-composer.component';

describe('ChatComposerComponent', () => {
  let fixture: ComponentFixture<ChatComposerComponent>;
  let textarea: HTMLTextAreaElement;
  let sent: string[];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ChatComposerComponent],
      providers: [
        { provide: AgentService, useValue: {} },
        { provide: ErrorHandlerService, useValue: {} },
      ],
    });
    fixture = TestBed.createComponent(ChatComposerComponent);
    sent = [];
    fixture.componentInstance.sent.subscribe((text) => sent.push(text));
    fixture.detectChanges();
    textarea = fixture.nativeElement.querySelector('textarea');
  });

  function voiceInput() {
    return fixture.debugElement.query(By.directive(VoiceInputComponent));
  }

  function type(text: string): void {
    textarea.value = text;
    textarea.dispatchEvent(new Event('input'));
  }

  it('appends dictated text after what was typed, without sending it', () => {
    type('Hola,');

    voiceInput().triggerEventHandler('transcribed', 'agenda a Juan mañana');
    fixture.detectChanges();

    expect(textarea.value).toBe('Hola, agenda a Juan mañana');
    expect(document.activeElement).toBe(textarea);
    expect(sent).toEqual([]);
  });

  it('keeps the dictated text within the message limit', () => {
    type('a'.repeat(MESSAGE_MAX_LENGTH - 2));

    voiceInput().triggerEventHandler('transcribed', 'texto largo');

    expect(textarea.value.length).toBe(MESSAGE_MAX_LENGTH);
  });

  it('hides the text field and the send button while recording', () => {
    voiceInput().triggerEventHandler('recordingChange', true);
    fixture.detectChanges();

    expect(textarea.classList).toContain('hidden');
    expect(fixture.nativeElement.querySelector('button[aria-label="Enviar mensaje"]')).toBeNull();
  });
});
