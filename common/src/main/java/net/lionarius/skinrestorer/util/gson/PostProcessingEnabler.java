package net.lionarius.skinrestorer.util.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class PostProcessingEnabler implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        var adapter = gson.getDelegateAdapter(this, type);
        
        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                adapter.write(out, value);
            }
            
            @Override
            public T read(JsonReader in) throws IOException {
                var value = adapter.read(in);
                
                if (value instanceof GsonPostProcessable postProcessable)
                    postProcessable.gsonPostProcess();
                
                return value;
            }
        };
    }
}
