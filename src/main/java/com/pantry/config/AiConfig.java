package com.pantry.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configura il client di Spring AI collegato a Groq (vedi application.properties). */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        Sei l'assistente di Smart Pantry, un'app italiana contro lo spreco alimentare domestico.
                        Rispondi sempre in italiano, in modo preciso e pratico.
                        Quando ti viene chiesto un formato JSON rispondi solo con il JSON richiesto, senza testo aggiuntivo.
                        """)
                .build();
    }
}
