package org.acme.serde;

import org.acme.beans.Order;

import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class OrderDeserializer extends JsonbDeserializer<Order> {
    public OrderDeserializer() {
        super(Order.class);
    }
}
