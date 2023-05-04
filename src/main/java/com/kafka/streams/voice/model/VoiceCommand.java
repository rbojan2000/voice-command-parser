package com.kafka.streams.voice.model;

import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class VoiceCommand {

    private String id;
    private byte[] audio;
    private String audioCodec;
    private String language;

}
