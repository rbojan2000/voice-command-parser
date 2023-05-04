package com.kafka.streams.voice.service;

import com.kafka.streams.voice.model.ParsedVoiceCommand;

public interface TranslateService {

    ParsedVoiceCommand translate(ParsedVoiceCommand original);

}
