import com.kafka.streams.voice.VoiceCommandParserTopology;
import com.kafka.streams.voice.config.StreamsConfiguration;
import com.kafka.streams.voice.service.MockSttClient;
import com.kafka.streams.voice.service.MockTranslateClient;
import org.apache.kafka.streams.KafkaStreams;

public class VoiceCommandParserApp {
    public static void main(String[] args) {
        VoiceCommandParserTopology voiceCommandParserTopology = new VoiceCommandParserTopology(new MockTranslateClient(), new MockSttClient(), 0.98);

        StreamsConfiguration streamsConfiguration = new StreamsConfiguration();
        KafkaStreams kafkaStreams = new KafkaStreams(voiceCommandParserTopology.createTopology(), streamsConfiguration.streamsConfiguration());
        kafkaStreams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
    }
}
