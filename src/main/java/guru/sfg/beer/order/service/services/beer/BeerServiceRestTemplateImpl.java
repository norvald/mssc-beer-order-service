package guru.sfg.beer.order.service.services.beer;

import guru.sfg.common.model.BeerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class BeerServiceRestTemplateImpl implements BeerService {
    public static final String BEER_PATH = "/api/v1/beer/";
    public static final String BEERUPC_PATH = "/api/v1/beerUpc/";
    private final RestTemplate restTemplate;

    @Value("${sfg.brewery.beer-service-host}")
    private String beerServiceHost;

    public void setBeerServiceHost(String beerServiceHost) {
        this.beerServiceHost = beerServiceHost;
    }

    public BeerServiceRestTemplateImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public Optional<BeerDto> getBeerById(UUID beerId) {
        log.debug("Calling beer service: "+ beerServiceHost + BEER_PATH+", "+beerId);

        return Optional.ofNullable(restTemplate.getForObject(beerServiceHost+BEER_PATH+beerId.toString(), BeerDto.class));
    }

    @Override
    public Optional<BeerDto> getBeerByUpc(String upc) {
        log.debug("Calling beer service: "+beerServiceHost + BEERUPC_PATH+", "+upc);

        return Optional.ofNullable(restTemplate.getForObject(beerServiceHost+BEERUPC_PATH+upc, BeerDto.class));
    }
}
