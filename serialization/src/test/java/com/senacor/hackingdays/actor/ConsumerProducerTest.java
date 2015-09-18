package com.senacor.hackingdays.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Stopwatch;
import com.senacor.hackingdays.serialization.data.generate.ProfileGenerator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static com.senacor.hackingdays.config.ConfigHelper.createConfig;
import static junitparams.JUnitParamsRunner.$;

@RunWith(JUnitParamsRunner.class)
public class ConsumerProducerTest {


    public static final int COUNT = 1000000;

    @Test
    @Parameters(method = "serializers")
    public void sendMessages(String serializerName, String fqcn) throws Exception {
        ActorSystem actorSystem = ActorSystem.create("producer-consumer-actorsystem", overrideConfig(serializerName, fqcn));
        ActorRef consumer = actorSystem.actorOf(Props.create(ConsumerActor.class, () -> new ConsumerActor()), "consumer");
        ActorRef producer = actorSystem.actorOf(Props.create(ProducerActor.class, () -> new ProducerActor(consumer)), "producer");

        Timeout timeout = Timeout.apply(120, TimeUnit.SECONDS);

        Stopwatch stopwatch = Stopwatch.createStarted();
        Future<Object> ask = Patterns.ask(producer, new GenerateMessages(COUNT), timeout);
        Await.result(ask, timeout.duration());
        stopwatch.stop();
        actorSystem.shutdown();
        actorSystem.awaitTermination();
        System.err.println(String.format("Sending %s dating profiles with %s took %s millis.", COUNT, serializerName, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }

    private Config overrideConfig(String serializerName, String fqcn) {
        String configSnippet = String.format("akka {\n" +
                "  actor {\n" +
                "    serializers {\n" +
                "      %s = \"%s\"\n" +
                "    }\n" +
                "\n" +
                "    serialization-bindings {\n" +
                "      \"com.senacor.hackingdays.serialization.thirftdata.Profile\" = %s\n" +
                "    }\n" +
                "  }\n" +
                "}", serializerName, fqcn, serializerName);

        Config overrides = ConfigFactory.parseString(configSnippet);
        return overrides.withFallback(ConfigFactory.load());
    }

    @SuppressWarnings("unusedDeclaration")
    static Object[] serializers() {
        return $(
                $("thrifttuple", "com.senacor.hackingdays.serializer.ThriftSerializerTTuple"),
                $("thriftbinary", "com.senacor.hackingdays.serializer.ThriftSerializerTBinary"),
                $("thriftcompact", "com.senacor.hackingdays.serializer.ThriftSerializerTCompact"),
                $("thriftjson", "com.senacor.hackingdays.serializer.ThriftSerializerTJSON"),
                $("thriftsimplejson", "com.senacor.hackingdays.serializer.ThriftSerializerTSimpleJSON"),
                $("thrifttuple", "com.senacor.hackingdays.serializer.ThriftSerializerTTuple"),
                $("java", "akka.serialization.JavaSerializer"),
                /* $("json", "com.senacor.hackingdays.serializer.JacksonSerializer"),  */
                $("gson", "com.senacor.hackingdays.serializer.GsonSerializer"),
                $("gson2", "com.senacor.hackingdays.serializer.GsonSerializer2"),
                $("fast-ser", "com.senacor.hackingdays.serializer.FastSerializer")
        );
    }

    @Test
    @Ignore
    public void writeXmlFile() throws Exception {
        /*
        try(XMLProfileWriter writer = new XMLProfileWriter(new File("src/main/resources/database.xml"))) {
            ProfileGenerator generator = ProfileGenerator.newInstance(1_000_000);
            generator.stream().forEach(writer::write);

        } */
    }

}