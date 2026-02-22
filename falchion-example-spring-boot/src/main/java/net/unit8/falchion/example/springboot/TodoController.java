package net.unit8.falchion.example.springboot;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/todos")
public class TodoController {
    private final Map<Long, Todo> todos = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @GetMapping
    public List<Todo> list() {
        return new ArrayList<>(todos.values());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> get(@PathVariable Long id) {
        Todo todo = todos.get(id);
        if (todo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(todo);
    }

    @PostMapping
    public ResponseEntity<Todo> create(@RequestBody Todo todo) {
        long id = idGenerator.getAndIncrement();
        todo.setId(id);
        todos.put(id, todo);
        return ResponseEntity.status(HttpStatus.CREATED).body(todo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Todo> update(@PathVariable Long id, @RequestBody Todo todo) {
        if (!todos.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        todo.setId(id);
        todos.put(id, todo);
        return ResponseEntity.ok(todo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (todos.remove(id) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
