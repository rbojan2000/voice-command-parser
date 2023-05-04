package com.kafka.streams.voice;

import com.kafka.streams.voice.model.ParsedVoiceCommand;
import com.kafka.streams.voice.model.VoiceCommand;
import com.kafka.streams.voice.serdes.JsonSerde;
import com.kafka.streams.voice.service.SpeechToTextService;
import com.kafka.streams.voice.service.TranslateService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceCommandParserTopologyTest {

    @Mock
    private SpeechToTextService speechToTextService;
    @Mock
    private TranslateService translateService;
    private VoiceCommandParserTopology voiceCommandParserTopology;
    private TopologyTestDriver topologyTestDriver;
    private TestInputTopic<String, VoiceCommand> voiceCommandTestInputTopic;
    private TestOutputTopic<String, ParsedVoiceCommand> recognizedCommandsOutputTopic;
    private TestOutputTopic<String, ParsedVoiceCommand> unrecognizedCommandsOutputTopic;


    @BeforeEach
    void setUp() {

        voiceCommandParserTopology = new VoiceCommandParserTopology(translateService, speechToTextService, 0.90);
        Topology topology = voiceCommandParserTopology.createTopology();

        Properties prop = new Properties();
        prop.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        prop.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");

        topologyTestDriver = new TopologyTestDriver(topology, prop);
        JsonSerde<VoiceCommand> voiceCommandJsonSerde = new JsonSerde<>(VoiceCommand.class);

        JsonSerde<ParsedVoiceCommand> parsedVoiceCommandJsonSerde = new JsonSerde<>(ParsedVoiceCommand.class);

        voiceCommandTestInputTopic = topologyTestDriver.createInputTopic(VoiceCommandParserTopology.VOICE_COMMANDS_TOPIC, Serdes.String().serializer(), voiceCommandJsonSerde.serializer());

        recognizedCommandsOutputTopic = topologyTestDriver.createOutputTopic(VoiceCommandParserTopology.RECOGNIZED_COMMANDS_TOPIC, Serdes.String().deserializer(), parsedVoiceCommandJsonSerde.deserializer());
        unrecognizedCommandsOutputTopic = topologyTestDriver.createOutputTopic(VoiceCommandParserTopology.UNRECOGNIZED_COMMAND_TOPIC, Serdes.String().deserializer(), parsedVoiceCommandJsonSerde.deserializer());

    }

    @Test
    @DisplayName("Given an English voice command, When processed correctly Then I receive a ParsedVoiceCommand in the recognnized-commands topic.")
    void testScenario1() {
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        VoiceCommand voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCodec("FLAC")
                .language("en-US")
                .build();

        ParsedVoiceCommand parsedVoiceCommand1 = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .language("en-US")
                .probability(0.95)
                .textCommand("call john")
                .build();
        given(speechToTextService.speechToText(voiceCommand)).willReturn(parsedVoiceCommand1);
        voiceCommandTestInputTopic.pipeInput(voiceCommand);

        ParsedVoiceCommand parsedVoiceCommand = recognizedCommandsOutputTopic.readRecord().value();

        assertEquals(voiceCommand.getId() ,parsedVoiceCommand.getId());
        assertEquals( "call john" ,parsedVoiceCommand.getTextCommand());

    }

    @Test
    @DisplayName("Given a non-English voice command, When processed correctly Then I receive a ParsedVoiceCommand in the recognnized-commands topic.")
    void testScenario2() {
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        VoiceCommand voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCodec("FLAC")
                .language("es-AR")
                .build();

        ParsedVoiceCommand parsedVoiceCommand1 = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .textCommand("llamar a Juan")
                .probability(0.98)
                .language("es-AR")
                .build();
        given(speechToTextService.speechToText(voiceCommand)).willReturn(parsedVoiceCommand1);

        ParsedVoiceCommand translatedParsedcommand = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .textCommand("call Juan")
                .language("en-US")
                .build();

        given(translateService.translate(parsedVoiceCommand1)).willReturn(translatedParsedcommand);
        voiceCommandTestInputTopic.pipeInput(voiceCommand);

        ParsedVoiceCommand parsedVoiceCommand = recognizedCommandsOutputTopic.readRecord().value();

        assertEquals(voiceCommand.getId() ,parsedVoiceCommand.getId());
        assertEquals("call Juan" ,parsedVoiceCommand.getTextCommand());

    }


    @Test
    @DisplayName("Given a non-recognizable voice command, When processed correctly Then I receive a ParsedVoiceCommand in the unrecognnized-commands topic.")
    void testScenario3() {
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        VoiceCommand voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCodec("FLAC")
                .language("en-US")
                .build();

        ParsedVoiceCommand parsedVoiceCommand1 = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .language("en-US")
                .textCommand("call john")
                .probability(0.75)
                .build();
        given(speechToTextService.speechToText(voiceCommand)).willReturn(parsedVoiceCommand1);
        voiceCommandTestInputTopic.pipeInput(voiceCommand);

        ParsedVoiceCommand parsedVoiceCommand = unrecognizedCommandsOutputTopic.readRecord().value();

        assertEquals(voiceCommand.getId() ,parsedVoiceCommand.getId());

        assertTrue(recognizedCommandsOutputTopic.isEmpty());
        verify(translateService, never()).translate(any(ParsedVoiceCommand.class));
    }


    @Test
    @DisplayName("Given voice command that is too short (less than 10 bytes), When processed correctly Then I don't receive any command in any of the output topics.")
    public void scenario4() {
        byte[] randomBytes = new byte[9];
        new Random().nextBytes(randomBytes);
        VoiceCommand voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCodec("FLAC")
                .language("en-US")
                .build();

        voiceCommandTestInputTopic.pipeInput(voiceCommand);

        assertTrue(recognizedCommandsOutputTopic.isEmpty());
        assertTrue(unrecognizedCommandsOutputTopic.isEmpty());

        verify(speechToTextService, never()).speechToText(any(VoiceCommand.class));
        verify(translateService, never()).translate(any(ParsedVoiceCommand.class));

    }
}
