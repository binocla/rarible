package io.valuva.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.valuva.client.NeuronClient;
import io.valuva.models.*;
import io.valuva.models.Collection;
import lombok.SneakyThrows;
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
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public AllCollections getAllCollections() {
        Log.info("AllCollections triggered");
        AllCollections allCollections;
        try {
            Response response = baseTarget.path("collections")
                    .path("all")
                    .queryParam("size", 1000)
                    .request()
                    .get();
            allCollections = response.readEntity(AllCollections.class);
            while (allCollections.getContinuation() != null) {
                Log.info("Total: " + allCollections.getTotal());
                if (allCollections.getTotal() >= 5000) {
                    Log.info("break triggered");
                    break;
                }
                Log.info("Response " + allCollections.getContinuation());
                Response nextResponse = baseTarget.path("collections")
                        .path("all")
                        .queryParam("continuation", URLEncoder.encode(allCollections.getContinuation(), StandardCharsets.UTF_8))
                        .queryParam("size", 1000)
                        .request()
                        .get();
                AllCollections newResponse = nextResponse.readEntity(AllCollections.class);
                Log.info("New Response continuation " + newResponse.getContinuation());
                Log.info("New Response total " + newResponse.getTotal());
                if (newResponse.getCollections() == null) {
                    break;
                }
                List<io.valuva.models.Collection> collections = allCollections.getCollections();
                collections.addAll(newResponse.getCollections());
                allCollections.setCollections(allCollections.getCollections().stream().filter(x -> x.getMeta() != null).filter(x -> x.getMeta().getContent() != null && !x.getMeta().getContent().isEmpty()).collect(Collectors.toList()));
                allCollections.setTotal(allCollections.getTotal() + newResponse.getTotal());
                allCollections.setContinuation(newResponse.getContinuation());
            }
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
        } catch (Exception e) {
            Log.error("Something went wrong: ", e);
            throw new BadRequestException(e.getMessage());
        }

        return handleItems(allItems);
    }

    private AllItems handleItems(AllItems allItems) {
        Map<Attribute, Long> totalMap = allItems.getItems().stream()
                .filter(x -> x.getMeta() != null)
                .filter(x -> !x.getMeta().getAttributes().isEmpty())
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
            if (i.getMeta() == null || i.getMeta().getAttributes() == null || i.getMeta().getAttributes().isEmpty()) {
                continue;
            }
            Map<Attribute, Long> map = i.getMeta().getAttributes().stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            i.setUniqueProperties(map.size());
        }
        allItems.setTotalUniqueProperties(mapOfUniqueProperties.size());
        for (Item i : allItems.getItems()) {
            if (i.getMeta() == null || i.getMeta().getAttributes() == null || i.getMeta().getAttributes().isEmpty()) {
                continue;
            }
            i.getMeta().setAttributes(i.getMeta().getAttributes().stream().map(attr -> new Attribute(attr.getKey(), attr.getValue(), (totalMap.get(attr) / (double) mapOfUniqueProperties.get(attr.getKey())), (1 - (totalMap.get(attr) / (double) mapOfUniqueProperties.get(attr.getKey()))) / (totalMap.get(attr) / (double) mapOfUniqueProperties.get(attr.getKey())), null, null)).collect(Collectors.toList()));
            List<Attribute> attributeList = i.getMeta().getAttributes();
            Map<String, Long> mapOfItemProperties = i.getMeta().getAttributes().stream()
                    .map(Attribute::getKey)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            for (String k : mapOfUniqueProperties.keySet()) {
                if (!mapOfItemProperties.containsKey(k)) {
                    attributeList.add(new Attribute(k, "None", null, null, null, null));
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
            if (i.getMeta() == null || i.getMeta().getAttributes() == null || i.getMeta().getAttributes().isEmpty()) {
                continue;
            }
            i.getMeta().setAttributes(i.getMeta().getAttributes().stream().map(attr -> new Attribute(attr.getKey(), attr.getValue(), (it.get(attr) / (double) unIt.get(attr.getKey())), (1 - (it.get(attr) / (double) unIt.get(attr.getKey()))) / (it.get(attr) / (double) unIt.get(attr.getKey())), null, null)).collect(Collectors.toList()));
            for (Attribute attribute : i.getMeta().getAttributes()) {
                rare += Math.pow(attribute.getRare(), 2);
            }
            i.setRare(Math.sqrt(rare));
            rare = 0.0;
        }
        return allItems;
    }

    @CacheResult(cacheName = "allItems")
    public List<FinalResult> getAllItems() {
        AllCollections allCollections = getAllCollections();
        return getFinalResults(allCollections);
    }

    @SneakyThrows
    private List<FinalResult> getFinalResults(AllCollections allCollections) {
        Log.info("FinalResults triggered " + allCollections.getTotal());
        List<AllItems> list = new ArrayList<>();
        for (Collection c : allCollections.getCollections()) {
            List<Item> itemList = new ArrayList<>();
            AllItems allItems = allItems(c.getId());
            if (allItems.getItems().isEmpty() || allItems.getItems().size() == 1) {
                continue;
            }
            for (int i = 0; i < allItems.getItems().size(); i++) {
                if (allItems.getItems().get(i).getRare() != null) {
                    itemList.add(allItems.getItems().get(i));
                }
            }
            allItems.setItems(itemList);
            list.add(allItems);
        }
        List<FinalResult> ls = list.parallelStream().filter(x -> x.getItems() != null && !x.getItems().isEmpty()).flatMap(x -> x.getItems().parallelStream().map(q -> new FinalResult(q.getCollection(), q.getId(), q.getBestSellOrder() == null ? null : q.getBestSellOrder().getMakePriceUsd(), q.getRare(), q.getMeta().getAttributes(), null, q.getCreators(), q.getBestBidOrder(), q.getAuctions(), q.getTotalStock(), q.getLastSale()))).distinct().collect(Collectors.toList());
        PrintWriter pw = new PrintWriter("dataNew.json");
        pw.println(new ObjectMapper().writeValueAsString(ls));
        pw.close();
        return ls;
    }

    @CacheResult(cacheName = "predictPrice")
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
        AllItems allItems = allItems(collection);

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

    public AllItems allItems(@NotBlank String collection) {
        Response response = baseTarget.path("items")
                .path("byCollection")
                .queryParam("collection", collection)
                .queryParam("size", 1000)
                .request()
                .get();
        AllItems allItems = response.readEntity(AllItems.class);
        Log.info(allItems.getContinuation());
        while (allItems.getContinuation() != null) {
            Log.info("Response " + allItems.getContinuation());
            Response nextResponse = baseTarget.path("items")
                    .path("byCollection")
                    .queryParam("collection", collection)
                    .queryParam("continuation", URLEncoder.encode(allItems.getContinuation(), StandardCharsets.UTF_8))
                    .queryParam("size", 1000)
                    .request()
                    .get();
            AllItems newResponse = nextResponse.readEntity(AllItems.class);
            Log.info("New Response " + newResponse.getContinuation());
            Log.info("New Response " + newResponse.getTotal());
            if (newResponse.getItems() == null) {
                break;
            }
            List<Item> listOfItems = allItems.getItems();
            listOfItems.addAll(newResponse.getItems());
            allItems.setItems(listOfItems);
            allItems.setTotal(allItems.getTotal() + newResponse.getTotal());
            allItems.setContinuation(newResponse.getContinuation());
        }
        return handleItems(allItems);
    }

    @PreDestroy
    void closeClient() {
        client.close();
    }
}
