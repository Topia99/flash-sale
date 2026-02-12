package com.flashSale.order.client.catalog;

import com.flashSale.order.dto.TicketResponse;
import com.flashSale.order.exception.CatalogErrorException;
import com.flashSale.order.exception.CatalogTimeoutException;
import com.flashSale.order.exception.NotFoundException;
import com.flashSale.order.exception.TicketNotFoundException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class CatalogClientImpl implements CatalogClient{
    private final RestClient client;

    public CatalogClientImpl(){
        this.client = RestClient.builder()
                .baseUrl("http://localhost:8082")
                .build();
    }

    @Override
    public TicketResponse getTicket(Long ticketId) {
        try{
            return client.get()
                    .uri("/catalog/tickets/{id}", ticketId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) ->{
                        // Not Found
                        throw new TicketNotFoundException("Ticket not found");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) ->{
                        // 500 error
                        throw new CatalogErrorException("CATALOG_5XX_" + res.getStatusCode().value());
                    })
                    .body(TicketResponse.class);
        } catch (ResourceAccessException e) {
            throw new CatalogTimeoutException("CATALOG_TIMEOUT", e);
        }
    }
}
