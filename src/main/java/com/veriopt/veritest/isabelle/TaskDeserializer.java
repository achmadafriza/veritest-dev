package com.veriopt.veritest.isabelle;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.TextNode;
import com.veriopt.veritest.isabelle.response.Task;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Source: https://programmerbruce.blogspot.com/2011/05/deserialize-json-with-jackson-into.html
* */
public class TaskDeserializer extends StdDeserializer<Task> {
    private final Set<Class<? extends Task>> classRegistry;
    private final Map<Set<String>, Class<? extends Task>> registry;

    public TaskDeserializer() {
        super(Task.class);
        this.classRegistry = new HashSet<>();
        this.registry = new HashMap<>();
    }

    public void register(Class<? extends Task> taskClass, String... fields) {
        if (this.classRegistry.contains(taskClass)) {
            throw new IllegalArgumentException("Duplicate class in the registry");
        }

        this.classRegistry.add(taskClass);
        this.registry.put(Set.of(fields), taskClass);
    }

    @Override
    public Task deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JacksonException {
        TreeNode root = parser.readValueAsTree();

        Iterable<String> iterable = root::fieldNames;
        Set<String> fields = StreamSupport.stream(iterable.spliterator(), true)
                .collect(Collectors.toSet());

        List<Set<String>> matchingFields = registry.entrySet().stream()
                .parallel()
                .filter(entry -> fields.containsAll(entry.getKey()))
                .map(Map.Entry::getKey)
                .toList();

        Class<? extends Task> targetClass = null;
        if (matchingFields.size() > 1) {
            throw MismatchedInputException.from(parser, Task.class, "Multiple possible classes to resolve");
        } else if (!matchingFields.isEmpty()) {
            targetClass = this.registry.get(matchingFields.get(0));
        } else if (!fields.contains("task")){
            throw MismatchedInputException.from(parser, Task.class, "Invalid Task");
        }

        Task task;
        if (targetClass == null) {
            TreeNode taskNode = root.get("task");
            if (!taskNode.isValueNode()) {
                throw MismatchedInputException.from(parser, Task.class, "Invalid Task Property");
            }

            if (taskNode instanceof TextNode textNode) {
                task = new Task();
                task.setId(textNode.textValue());
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            task = parser.getCodec().treeToValue(root, targetClass);
        }

        return task;
    }
}
