package com.senacor.hackingdays.actor;

import static com.senacor.hackingdays.config.ConfigHelper.createConfig;
import static junitparams.JUnitParamsRunner.$;

import java.io.File;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.serialization.SerializationExtension;
import akka.serialization.Serializer;
import akka.util.Timeout;
import com.google.common.base.Stopwatch;
import com.senacor.hackingdays.serialization.data.Profile;
import com.senacor.hackingdays.serialization.data.generate.ProfileGenerator;
import com.senacor.hackingdays.serialization.data.writer.XMLProfileWriter;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import scala.concurrent.Await;
import scala.concurrent.Future;

@RunWith(JUnitParamsRunner.class)
public class ConsumerProducerTest {

    public static final int COUNT = 100_000;

    @Test
    @Parameters(method = "serializers")
    public void sendMessages(String serializerName, String fqcn) throws Exception {
        ActorSystem actorSystem = ActorSystem.create("producer-consumer-actorsystem", createConfig(serializerName, fqcn));
        ActorRef consumer = actorSystem.actorOf(Props.create(ConsumerActor.class, () -> new ConsumerActor()), "consumer");
        ActorRef producer = actorSystem.actorOf(Props.create(ProducerActor.class, () -> new ProducerActor(consumer)), "producer");

        Timeout timeout = Timeout.apply(25, TimeUnit.SECONDS);

        Stopwatch stopwatch = Stopwatch.createStarted();
        Future<Object> ask = Patterns.ask(producer, new GenerateMessages(COUNT), timeout);
        Await.result(ask, timeout.duration());
        stopwatch.stop();
        actorSystem.shutdown();
        actorSystem.awaitTermination();
        System.err.println(
            String.format("Sending %s dating profiles with %s took %s millis.", COUNT, serializerName, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }

    @Test
    @Parameters(method = "serializerProtoBuf")
    public void sendMessagesProtoBuf(String serializerName, String fqcn) throws Exception {
        ActorSystem actorSystem = ActorSystem.create("producer-consumer-actorsystem-protobuf", createConfig(serializerName, fqcn));
        ActorRef consumer = actorSystem.actorOf(Props.create(ConsumerActorProto.class, () -> new ConsumerActorProto()), "consumer");
        ActorRef producer = actorSystem.actorOf(Props.create(ProducerActorProto.class, () -> new ProducerActorProto(consumer)), "producer");

        Timeout timeout = Timeout.apply(25, TimeUnit.SECONDS);

        Stopwatch stopwatch = Stopwatch.createStarted();
        Future<Object> ask = Patterns.ask(producer, new GenerateMessages(COUNT), timeout);
        Await.result(ask, timeout.duration());
        stopwatch.stop();
        actorSystem.shutdown();
        actorSystem.awaitTermination();
        System.err.println(String.format("Sending %s dating profiles with %s took %s millis.", COUNT, serializerName, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
    }

    @Test
    @Parameters(method = "serializers")
    public void calculateObjectSize(String serializerName, String fqcn) throws Exception {
        ActorSystem actorSystem = ActorSystem.create("producer-consumer-actorsystem", createConfig(serializerName, fqcn));

        Profile p = ProfileGenerator.newInstance(1).iterator().next();
        int length = SerializationExtension.get(actorSystem).serializerFor(Profile.class).toBinary(p).length;
        Thread.sleep(1000);
        actorSystem.shutdown();
        actorSystem.awaitTermination();

        System.err.println(String.format("Serializing a Profile with %s took %s bytes.", serializerName, length));
    }

    @Test
    @Parameters(method = "serializers")
    public void assertFields(String serializerName, String fqcn) throws Exception {
        ActorSystem actorSystem = ActorSystem.create("producer-consumer-actorsystem", createConfig(serializerName, fqcn));

        Serializer serializer = SerializationExtension.get(actorSystem).serializerFor(Profile.class);

        Profile input = ProfileGenerator.newInstance(1).iterator().next();
        Profile output = (Profile) serializer.fromBinary(serializer.toBinary(input), Profile.class);

        actorSystem.shutdown();
        actorSystem.awaitTermination();

        Assert.assertEquals(input.getActivity().getLastLogin(), output.getActivity().getLastLogin());
        Assert.assertEquals(input.getActivity().getLoginCount(), output.getActivity().getLoginCount());
        Assert.assertEquals(input.getAge(), output.getAge());
        Assert.assertEquals(input.getGender(), output.getGender());
        Assert.assertEquals(input.getLocation().getCity(), output.getLocation().getCity());
        Assert.assertEquals(input.getLocation().getState(), output.getLocation().getState());
        Assert.assertEquals(input.getLocation().getZip(), output.getLocation().getZip());
        Assert.assertEquals(input.getName(), output.getName());
        Assert.assertEquals(input.getRelationShip(), output.getRelationShip());
        Assert.assertEquals(input.getSeeking().getAgeRange().getLower(), output.getSeeking().getAgeRange().getLower());
        Assert.assertEquals(input.getSeeking().getAgeRange().getUpper(), output.getSeeking().getAgeRange().getUpper());
        Assert.assertEquals(input.getSeeking().getGender(), output.getSeeking().getGender());
    }

    @SuppressWarnings("unusedDeclaration")
    static Object[] serializers() {
        return $(
                $("java", "akka.serialization.JavaSerializer"),
                $("json", "com.senacor.hackingdays.serializer.JacksonSerializer"),
                $("gson", "com.senacor.hackingdays.serializer.GsonSerializer"),
                $("gson2", "com.senacor.hackingdays.serializer.GsonSerializer2"),
                $("xml", "com.senacor.hackingdays.serializer.XStreamXMLSerializer"),
                $("json-io", "com.senacor.hackingdays.serializer.JsonIoSerializer"),
                $("fast-ser", "com.senacor.hackingdays.serializer.FastSerializer"),
                $("kryo", "com.senacor.hackingdays.serializer.KryoSerializer"),
                $("unsafe", "com.senacor.hackingdays.serializer.UnsafeSerializer")
        );
    }
//    private Config overrideConfig(String serializerName, String fqcn) {
//        String configSnippet = String.format("akka {\n" +
//            "  actor {\n" +
//            "    serializers {\n" +
//            "      %s = \"%s\"\n" +
//            "    }\n" +
//            "\n" +
//            "    serialization-bindings {\n" +
//            "      \"com.senacor.hackingdays.serialization.data.Profile\" = %s\n" +
//            "    }\n" +
//            "  }\n" +
//            "}", serializerName, fqcn, serializerName);
//
//        Config overrides = ConfigFactory.parseString(configSnippet);
//        return overrides.withFallback(ConfigFactory.load());
//    }

    @SuppressWarnings("unusedDeclaration")
    static Object[] serializerProtoBuf() {
        return $(
                $("protoBuf", "com.senacor.hackingdays.serializer.ProtoBufSerilalizer")
        );
    }


}