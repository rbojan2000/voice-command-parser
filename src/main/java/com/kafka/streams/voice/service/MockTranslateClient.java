package com.kafka.streams.voice.service;


import com.kafka.streams.voice.model.ParsedVoiceCommand;

public class MockTranslateClient implements TranslateService {

    public ParsedVoiceCommand translate(ParsedVoiceCommand original) {
        return ParsedVoiceCommand.builder()
                .id(original.getId())
                .textCommand("call juan")
                .probability(original.getProbability())
                .language(original.getLanguage())
                .build();
    }
}