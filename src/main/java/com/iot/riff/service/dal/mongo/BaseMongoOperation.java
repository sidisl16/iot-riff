package com.iot.riff.service.dal.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Sorts;
import org.bson.types.ObjectId;

public abstract class BaseMongoOperation<T> {

    @Inject
    protected MongoClient mongoClient;

    protected abstract String getDatabaseName();

    protected abstract String getCollectionName();

    protected abstract Mapper<T> getMapper();

    protected MongoCollection<Document> getCollection() {
        return mongoClient.getDatabase(getDatabaseName()).getCollection(getCollectionName());
    }

    protected Object parseId(String id) {
        if (ObjectId.isValid(id)) {
            return new ObjectId(id);
        }
        return id;
    }

    public T get(String id) {
        Document doc = getCollection().find(Filters.eq("_id", parseId(id))).first();
        return doc != null ? getMapper().toModel().apply(doc) : null;
    }

    public T save(T model) {
        Document doc = getMapper().toDocument().apply(model);
        getCollection().insertOne(doc);
        return getMapper().toModel().apply(doc);
    }

    public List<T> list() {
        return getCollection().find().map(getMapper().toModel()::apply).into(new ArrayList<>());
    }

    public List<T> list(Bson filter, int limit, int page, String sort, String sortBy) {
        FindIterable<Document> query = getCollection().find(filter != null ? filter : new Document());

        if (limit > 0) {
            query.limit(limit);
            if (page > 0) {
                query.skip(page * limit);
            }
        }

        if (sortBy != null && !sortBy.isEmpty()) {
            if ("desc".equalsIgnoreCase(sort)) {
                query.sort(Sorts.descending(sortBy));
            } else {
                query.sort(Sorts.ascending(sortBy));
            }
        }

        return query.map(getMapper().toModel()::apply).into(new ArrayList<>());
    }

    public void delete(String id) {
        getCollection().deleteOne(Filters.eq("_id", parseId(id)));
    }

    public long count(Bson filter) {
        return getCollection().countDocuments(filter != null ? filter : new Document());
    }
}
