package app;

import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = {
        "controller"
})
public class App {
    public static void main(String[] args){
        SpringApplication.run(App.class, args);
    }

//    @Bean
//    public JDABuilder builder(@Value("${discord.token}") String token){
//        return JDABuilder.createDefault(token);
//    }
}
