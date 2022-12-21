package guru.sfg.beer.order.service.web.mappers;

import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.services.beer.BeerService;
import guru.sfg.brewery.model.BeerDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@Slf4j
public class BeerOrderLineMapperDecorator implements BeerOrderLineMapper {
    private BeerService beerService;
    private BeerOrderLineMapper mapper;

    @Autowired
    public void setBeerService(BeerService beerService) {
        this.beerService = beerService;
    }

    @Autowired
    public void setMapper(BeerOrderLineMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {
        BeerOrderLineDto beerOrderLineDto = mapper.beerOrderLineToDto(line);
        if(beerOrderLineDto.getUpc() != null) {
            Optional<BeerDto> beerDtoOptional = beerService.getBeerByUpc(beerOrderLineDto.getUpc());
            beerDtoOptional.ifPresent(beerDto -> {
                beerOrderLineDto.setBeerId(beerDto.getId());
                beerOrderLineDto.setBeerName(beerDto.getBeerName());
                beerOrderLineDto.setBeerStyle(beerDto.getBeerStyle().toString());
                //beerOrderLineDto.setUpc(beerDto.getUpc());
                beerOrderLineDto.setPrice(beerDto.getPrice());
            });
            return beerOrderLineDto;

        } else if(beerOrderLineDto.getBeerId() != null) {
            Optional<BeerDto> beerDtoOptional = beerService.getBeerById(beerOrderLineDto.getBeerId());

            beerDtoOptional.ifPresent(beerDto -> {
                //beerOrderLineDto.setBeerId(beerDto.getId());
                beerOrderLineDto.setBeerName(beerDto.getBeerName());
                beerOrderLineDto.setBeerStyle(beerDto.getBeerStyle().toString());
                beerOrderLineDto.setUpc(beerDto.getUpc());
                beerOrderLineDto.setPrice(beerDto.getPrice());
            });
        }
        return beerOrderLineDto;
    }

    @Override
    public BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto) {
        BeerOrderLine beerOrderLine = mapper.dtoToBeerOrderLine(dto);
        return beerOrderLine;
    }
}
