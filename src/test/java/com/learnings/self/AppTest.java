package com.learnings.self;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.learnings.self.handlers.UserHandler;
import org.junit.Test;

public class AppTest {
  @Test
  public void successfulResponse() {
    UserHandler app = new UserHandler();
    APIGatewayProxyResponseEvent result = app.handleRequest(null, null);
    assertEquals(200, result.getStatusCode().intValue());
    assertEquals("application/json", result.getHeaders().get("Content-Type"));
    String content = result.getBody();
    assertNotNull(content);
    assertTrue(content.contains("\"name\""));
    assertTrue(content.contains("\"location\""));
  }
}
