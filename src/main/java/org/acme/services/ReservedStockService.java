package org.acme.services;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.quarkus.kafka.client.serialization.JsonbSerde;
import org.acme.beans.Order;
import org.acme.beans.Product;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Produced;

@ApplicationScoped
public class ReservedStockService {

    public static final String ORDERS_TOPIC = "orders";
    public static final String SHIPMENTS_TOPIC = "shipments";
    public static final String RESERVED_STOCK_TOPIC = "reserved-stock";

    private final JsonbSerde<Order> orderSerde = new JsonbSerde<>(Order.class);
    private final JsonbSerde<Product> productSerde = new JsonbSerde<>(Product.class);

    @Produces
    public Topology buildTopology() {
        final StreamsBuilder builder = new StreamsBuilder();

        final KStream<String, Order> orders = builder.stream(
                ORDERS_TOPIC,
                Consumed.with(Serdes.String(), orderSerde));
        final KStream<String, Order> shipments = builder.stream(
                SHIPMENTS_TOPIC,
                Consumed.with(Serdes.String(), orderSerde));

        final KeyValueMapper<String,Order,Iterable<KeyValue<Product,Integer>>> orderToProductQuantitiesMapping =
            (orderId, order) -> 
                () -> Stream.of(order.getOrderEntries())
                    .map(e -> new KeyValue<Product,Integer>(e.getProduct(), e.getQuantity()))
                    .iterator();

        final KStream<Product,Integer> stockOrdered = orders.flatMap(orderToProductQuantitiesMapping);
        final KStream<Product,Integer> stockShipped = shipments.flatMap(orderToProductQuantitiesMapping);
        final KStream<Product,Integer> stockReserved = stockOrdered.merge(stockShipped.mapValues(q -> -q));

        stockReserved.to(RESERVED_STOCK_TOPIC, Produced.with(productSerde, Serdes.Integer()));
        
        return builder.build();
    }
    
}
