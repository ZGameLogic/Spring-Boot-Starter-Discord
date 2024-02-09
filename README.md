# Spring Boot Starter Discord
Spring boot starter discord is a spring initializer that integrates [JDA](https://github.com/discord-jda/JDA) into spring.

## Setup
1. Add this dependency to your pom.xml file
```xml
<dependency>
  <groupId>com.zgamelogic</groupId>
  <artifactId>spring-boot-starter-discord</artifactId>
  <version>1.0.0</version>
</dependency>
```
2. Get a discord application setup in the [Discord developer portal](https://discord.com/developers/) and get a bot token.
3. Place this bot token in your application.properties file with the key of `discord.token`
```properties 
discord.token=[Bot token here]
```
4. This also supports a couple more simple properties for JDA:
```properties
discord.gateway-intents
discord.cache-flags
discord.member-cache-policy
discord.event-passthrough
```
5. To create a controller for discord events, annotate a class with `@DiscordController`
6. To create a method to handle an event, add the `@DicordMapping` annotation over it. This method must include __one__ JDA event parameter to listen for. The annotation can also include Ids to filter out other calls and only handle the one in that singular method. Here is a button for example:

```java
import com.zgamelogic.annotations.DiscordController;
import com.zgamelogic.annotations.DiscordMapping;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

@DiscordController
public class DiscordListener {
    @DiscordMapping(Id = "test_button")
    private void buttonResponse(ButtonInteractionEvent event){
        event.reply("You pushed my test button!").queue();
    }
}
```