package com.senacor.hackingdays.serializer;

import akka.serialization.JSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.senacor.hackingdays.serialization.data.Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class KryoSerializer extends JSerializer {

    private Kryo kryo = new Kryo();

    public KryoSerializer() {
//        kryo.addDefaultSerializer(Range.class, new RangeSerializer());
//        kryo.addDefaultSerializer(Location.class, new LocationSerializer());
//        kryo.addDefaultSerializer(Gender.class, new GenderSerializer());
//        kryo.addDefaultSerializer(Seeking.class, new SeekingSerializer());
//        kryo.addDefaultSerializer(Activity.class, new ActivitySerializer());
        kryo.addDefaultSerializer(Profile.class, new com.senacor.hackingdays.serializer.kryo.sryll.ProfileSerializer());
        kryo.setReferences(false);
//        kryo.addDefaultSerializer(Profile.class, new com.senacor.hackingdays.serializer.ProfileSerializer(kryo));
    }

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        Input input = new Input(new ByteArrayInputStream(bytes));
        Object object = kryo.readObject(input, manifest);
        input.close();
        return object;
    }

    @Override
    public byte[] toBinary(Object o) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);
        kryo.writeObject(output, o);
        output.close();
        return outputStream.toByteArray();
    }

    @Override
    public int identifier() {
        return 0;
    }

    @Override
    public boolean includeManifest() {
        return true;
    }

}
