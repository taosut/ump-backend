package vn.ssdc.vnpt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

/**
 * Created by vietnq on 10/21/16.
 */
@SpringBootApplication(
        scanBasePackages = {"vn.ssdc.vnpt"},
        exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class}
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
