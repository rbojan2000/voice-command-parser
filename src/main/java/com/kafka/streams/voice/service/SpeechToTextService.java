package com.kafka.streams.voice.service;

import com.kafka.streams.voice.model.ParsedVoiceCommand;
import com.kafka.streams.voice.model.VoiceCommand;

public interface SpeechToTextService {
    ParsedVoiceCommand speechToText(VoiceCommand voiceCommand);
}
