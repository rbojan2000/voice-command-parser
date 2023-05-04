package com.kafka.streams.voice;
import com.kafka.streams.voice.model.ParsedVoiceCommand;
import com.kafka.streams.voice.model.VoiceCommand;
import com.kafka.streams.voice.serdes.JsonSerde;
import com.kafka.streams.voice.service.SpeechToTextService;
import com.kafka.streams.voice.service.TranslateService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Map;

public class VoiceCommandParserTopology {

    public static final String VOICE_COMMANDS_TOPIC = "voice-commands";
    public static final String RECOGNIZED_COMMANDS_TOPIC = "recognized-commands";
    public static final String  UNRECOGNIZED_COMMAND_TOPIC = "unrecognized-commands";
    private final SpeechToTextService speechToTextService;
    private final TranslateService translateService;
    private final Double certaintyTreshold;

    public VoiceCommandParserTopology(TranslateService translateService, SpeechToTextService speechToTextService, double certaintyTreshold) {
        this.speechToTextService = speechToTextService;
        this.translateService = translateService;
        this.certaintyTreshold = certaintyTreshold;
    }

    public Topology createTopology() {
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        JsonSerde<VoiceCommand> voiceJsonSerde = new JsonSerde<>(VoiceCommand.class);
        JsonSerde<ParsedVoiceCommand> parsedVoiceCommandJsonSerde = new JsonSerde<>(ParsedVoiceCommand.class);

        Map<String, KStream<String, ParsedVoiceCommand>> branches = streamsBuilder.stream(VOICE_COMMANDS_TOPIC, Consumed.with(Serdes.String(), voiceJsonSerde))
                .filter((key, value) -> value.getAudio().length >= 10)
                .mapValues((readOnlyKey, value) -> speechToTextService.speechToText(value))
                .split(Named.as("branches-"))
                .branch((key, value) -> value.getProbability() > certaintyTreshold, Branched.as("recognized"))
                .defaultBranch(Branched.as("unrecognized"));
        branches.get("branches-unrecognized")
                .to(UNRECOGNIZED_COMMAND_TOPIC, Produced.with(Serdes.String(), parsedVoiceCommandJsonSerde));


        Map<String, KStream<String, ParsedVoiceCommand>> streamsMap =  branches.get("branches-recognized")
                .split(Named.as("language-"))
                .branch((key, value) -> value.getLanguage().startsWith("en"), Branched.as("english"))
                .defaultBranch(Branched.as("non-english"));

        streamsMap.get("language-non-english")
                .mapValues((readOnlyKey, value) -> translateService.translate(value))
                .merge(streamsMap.get("language-english"))
                .to(RECOGNIZED_COMMANDS_TOPIC, Produced.with(Serdes.String(), parsedVoiceCommandJsonSerde));

        return streamsBuilder.build();
    }
}
