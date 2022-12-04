package guru.sfg.beer.events;

import guru.sfg.beer.order.service.services.beer.model.BeerDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeerEvent implements Serializable {
    static final long serialVersionUID = 8149336873110298051L;

    private BeerDto beerDto;
}
