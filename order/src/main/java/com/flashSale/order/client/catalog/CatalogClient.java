package com.flashSale.order.client.catalog;

import com.flashSale.order.dto.TicketResponse;

public interface CatalogClient {
    TicketResponse getTicket(Long ticketId);
}
