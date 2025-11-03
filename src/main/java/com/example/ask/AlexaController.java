package com.example.ask;

import com.amazon.ask.Skill;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.ResponseEnvelope;
import com.amazon.ask.util.impl.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AlexaController {

    private final Skill skill;
    private final ObjectMapper mapper = ObjectMapperFactory.getMapper();

    public AlexaController(Skill skill) {
        this.skill = skill;
    }

    @PostMapping(
            value = "/alexa",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> handle(@RequestBody byte[] body) throws Exception {
        RequestEnvelope req = mapper.readValue(body, RequestEnvelope.class);

        ResponseEnvelope resp = skill.invoke(req);

        byte[] json = mapper.writeValueAsBytes(resp);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }
}
