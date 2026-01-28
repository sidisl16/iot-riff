package com.iot.riff.service.dal.mongo;

import java.util.function.Function;

import org.bson.Document;

public record Mapper<T>(Function<Document, T> toModel, Function<T, Document> toDocument) {
}