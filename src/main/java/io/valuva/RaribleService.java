package io.valuva;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class RaribleService {
    @Inject
    @RestClient
    NeuronClient neuronClient;
    private Client client;
    private WebTarget baseTarget;

    @PostConstruct
    void initClient() {
        client = ClientBuilder.newClient();
        baseTarget = client.target("https://api.rarible.org/v0.1");
    }

    public AllCollections getAllCollections(String blockchains, String continuation, @Positive Integer size) {
        AllCollections allCollections;
        try {
            Response response = baseTarget.path("collections")
                    .path("all")
                    .queryParam("blockchains", Optional.ofNullable(blockchains).orElse(""))
                    .queryParam("continuation", Optional.ofNullable(continuation).orElse(""))
                    .queryParam("size", Optional.ofNullable(size).orElse(50))
                    .request()
                    .get();
            allCollections = response.readEntity(AllCollections.class);
            allCollections.setCollections(allCollections.getCollections().stream().filter(x -> x.getMeta().getContent() != null && !x.getMeta().getContent().isEmpty()).collect(Collectors.toList()));
            allCollections.setRemaining(allCollections.getCollections().size());
            Log.debug("AllCollections object is: " + allCollections);
        } catch (Exception e) {
            Log.error("Something went wrong: ", e);
            throw new BadRequestException(e.getMessage());
        }
        return allCollections;
    }

    @CacheResult(cacheName = "collection")
    public AllItems getItemsByCollection(@NotBlank String collection, String continuation, @Positive Integer size) {
        AllItems allItems;
        try {
            Response response = baseTarget.path("items")
                    .path("byCollection")
                    .queryParam("collection", collection)
                    .queryParam("continuation", Optional.ofNullable(continuation).orElse(""))
                    .queryParam("size", Optional.ofNullable(size).orElse(50))
                    .request()
                    .get();
            allItems = response.readEntity(AllItems.class);
            if (allItems.getItems() == null) {
                return new AllItems();
            }
            Map<Attribute, Long> totalMap = allItems.getItems().stream()
                    .filter(x -> x.getMeta() != null)
                    .flatMap(x -> x.getMeta().getAttributes().stream())
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            Map<String, Long> mapOfUniqueProperties = new HashMap<>();
            for (Map.Entry<Attribute, Long> attribute : totalMap.entrySet()) {
                if (!mapOfUniqueProperties.containsKey(attribute.getKey().getKey())) {
                    mapOfUniqueProperties.put(attribute.getKey().getKey(), attribute.getValue());
                } else {
                    mapOfUniqueProperties.put(attribute.getKey().getKey(), mapOfUniqueProperties.get(attribute.getKey().getKey()) + attribute.getValue());
                }
            }

            for (Item i : allItems.getItems()) {
                if (i.getMeta() == null) {
                    continue;
                }
                Map<Attribute, Long> map = i.getMeta().getAttributes().stream()
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                i.setUniqueProperties(map.size());
            }
            allItems.setTotalUniqueProperties(mapOfUniqueProperties.size());
            for (Item i : allItems.getItems()) {
                if (i.getMeta() == null) {
                    continue;
                }
                i.getMeta().setAttributes(i.getMeta().getAttributes().stream().map(attr -> new Attribute(attr.getKey(), attr.getValue(), (totalMap.get(attr) / (double) mapOfUniqueProperties.get(attr.getKey())), (1 - (totalMap.get(attr) / (double) mapOfUniqueProperties.get(attr.getKey()))) / (totalMap.get(attr) / (double) mapOfUniqueProperties.get(attr.getKey())))).collect(Collectors.toList()));
                List<Attribute> attributeList = i.getMeta().getAttributes();
                Map<String, Long> mapOfItemProperties = i.getMeta().getAttributes().stream()
                        .map(Attribute::getKey)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                for (String k : mapOfUniqueProperties.keySet()) {
                    if (!mapOfItemProperties.containsKey(k)) {
                        attributeList.add(new Attribute(k, "None", null, null));
                    }
                }
                i.getMeta().setAttributes(attributeList);
            }
            Map<Attribute, Long> it = allItems.getItems().stream()
                    .filter(x -> x.getMeta() != null)
                    .flatMap(x -> x.getMeta().getAttributes().stream())
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            Map<String, Long> unIt = new HashMap<>();
            for (Map.Entry<Attribute, Long> attribute : it.entrySet()) {
                if (!unIt.containsKey(attribute.getKey().getKey())) {
                    unIt.put(attribute.getKey().getKey(), attribute.getValue());
                } else {
                    unIt.put(attribute.getKey().getKey(), unIt.get(attribute.getKey().getKey()) + attribute.getValue());
                }
            }
            double rare = 0.0;
            for (Item i : allItems.getItems()) {
                if (i.getMeta() == null) {
                    continue;
                }
                i.getMeta().setAttributes(i.getMeta().getAttributes().stream().map(attr -> new Attribute(attr.getKey(), attr.getValue(), (it.get(attr) / (double) unIt.get(attr.getKey())), (1 - (it.get(attr) / (double) unIt.get(attr.getKey()))) / (it.get(attr) / (double) unIt.get(attr.getKey())))).collect(Collectors.toList()));
                for (Attribute attribute : i.getMeta().getAttributes()) {
                    rare += Math.pow(attribute.getRare(), 2);
                }
                i.setRare(Math.sqrt(rare));
                rare = 0.0;
            }
        } catch (Exception e) {
            Log.error("Something went wrong: ", e);
            throw new BadRequestException(e.getMessage());
        }
        return allItems;
    }

    @CacheResult(cacheName = "allItems")
    public List<FinalResult> getAllItems(@Positive Integer sizeOfCollections, @Positive Integer sizeOfItems) {
        AllCollections allCollections = getAllCollections(null, null, sizeOfCollections);
        List<AllItems> list = new ArrayList<>();
        for (Collection c : allCollections.getCollections()) {
            List<Item> itemList = new ArrayList<>();
            AllItems allItems = getItemsByCollection(c.getId(), null, sizeOfItems);
            if (allItems.getItems().isEmpty() || allItems.getItems().size() == 1) {
                continue;
            }
            for (int i = 0; i < allItems.getItems().size(); i++) {
                if (allItems.getItems().get(i).getRare() != null && allItems.getItems().get(i).getBestSellOrder() != null) {
                    itemList.add(allItems.getItems().get(i));
                }
            }
            allItems.setItems(itemList);
            list.add(allItems);
        }
        return list.parallelStream().filter(x -> x.getItems() != null && !x.getItems().isEmpty()).flatMap(x -> x.getItems().parallelStream().map(q -> new FinalResult(q.getCollection(), q.getId(), q.getBestSellOrder().getMakePriceUsd(), q.getRare(), q.getMeta().getAttributes()))).distinct().collect(Collectors.toList());
    }

    public NeuronModel predictPrice(@NotBlank String url) {
        Log.info(url);
        Log.info(Arrays.toString(url.split("%2F+|\\?+|%3F")));
        url = "ETHEREUM%3A" + url.split("%2F+|\\?+|%3F")[4];
        Log.info(url);
        Response response = baseTarget.path("items")
                .path(url)
                .request()
                .get();
        ObjectNode objectNode = response.readEntity(ObjectNode.class);
        String collection = objectNode.get("collection").asText();
        AllItems allItems = getItemsByCollection(collection, null, 1000);

        while (true) {
            Log.info(objectNode.get("id").asText());
            try {
                if (allItems.getItems() == null) {
                    throw new BadRequestException("Try to use another NFT :(");
                }
                Item item = allItems.getItems().stream().filter(x -> x.getId().equalsIgnoreCase(objectNode.get("id").asText())).findFirst().get();
                NeuronRequest neuronRequest = NeuronRequest.builder()
                        .attributes(item.getMeta().getAttributes())
                        .id(item.getId())
                        .rare(item.getRare())
                        .collection(item.getCollection())
                        .build();
                ValueNode valueNode = new ObjectMapper().createObjectNode().pojoNode(neuronRequest);
                Log.info(valueNode);
                return neuronClient.getResult(valueNode);
            } catch (NoSuchElementException e) {
                allItems = getItemsByCollection(collection, allItems.getContinuation(), 1000);
            }
        }
    }

    @PreDestroy
    void closeClient() {
        client.close();
    }
}
