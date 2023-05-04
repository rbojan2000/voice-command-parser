package com.kafka.streams.voice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.kafka.streams.kstream.Produced;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ParsedVoiceCommand {

    private String id;
    private String textCommand;
    private Double probability;
    private String language;

}
