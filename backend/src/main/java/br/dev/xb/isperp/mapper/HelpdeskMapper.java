package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.entity.HelpdeskTicket;
import br.dev.xb.isperp.entity.TicketInteraction;
import br.dev.xb.isperp.service.HelpdeskService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface HelpdeskMapper {

    @Mapping(target = "category", expression = "java(toDtoCategory(ticket.getCategory()))")
    @Mapping(target = "priority", expression = "java(toDtoPriority(ticket.getPriority()))")
    @Mapping(target = "status", expression = "java(toDtoStatus(ticket.getStatus()))")
    @Mapping(target = "channel", expression = "java(toDtoChannel(ticket.getChannel()))")
    HelpdeskTicketResponse toResponse(HelpdeskTicket ticket);

    List<HelpdeskTicketResponse> toResponseList(List<HelpdeskTicket> tickets);

    @Mapping(target = "senderType", expression = "java(toDtoSenderType(interaction.getSenderType()))")
    TicketInteractionResponse toInteractionResponse(TicketInteraction interaction);

    List<TicketInteractionResponse> toInteractionResponseList(List<TicketInteraction> interactions);

    @Mapping(target = "category", expression = "java(toEntityCategory(request.getCategory()))")
    @Mapping(target = "priority", expression = "java(toEntityPriority(request.getPriority()))")
    @Mapping(target = "channel", expression = "java(toEntityChannel(request.getChannel()))")
    HelpdeskService.CreateTicketRequest toCreateServiceRequest(TicketCreateRequest request);

    @Mapping(target = "senderType", expression = "java(toEntitySenderType(request.getSenderType()))")
    HelpdeskService.AddInteractionRequest toAddInteractionServiceRequest(TicketInteractionCreateRequest request);

    default TicketCategory toDtoCategory(HelpdeskTicket.TicketCategory category) {
        if (category == null) return null;
        return TicketCategory.valueOf(category.name());
    }

    default HelpdeskTicket.TicketCategory toEntityCategory(TicketCategory category) {
        if (category == null) return null;
        return HelpdeskTicket.TicketCategory.valueOf(category.getValue());
    }

    default TicketPriority toDtoPriority(HelpdeskTicket.TicketPriority priority) {
        if (priority == null) return null;
        return TicketPriority.valueOf(priority.name());
    }

    default HelpdeskTicket.TicketPriority toEntityPriority(TicketPriority priority) {
        if (priority == null) return null;
        return HelpdeskTicket.TicketPriority.valueOf(priority.getValue());
    }

    default TicketStatus toDtoStatus(HelpdeskTicket.TicketStatus status) {
        if (status == null) return null;
        return TicketStatus.valueOf(status.name());
    }

    default HelpdeskTicket.TicketStatus toEntityStatus(TicketStatus status) {
        if (status == null) return null;
        return HelpdeskTicket.TicketStatus.valueOf(status.getValue());
    }

    default TicketChannel toDtoChannel(HelpdeskTicket.TicketChannel channel) {
        if (channel == null) return null;
        return TicketChannel.valueOf(channel.name());
    }

    default HelpdeskTicket.TicketChannel toEntityChannel(TicketChannel channel) {
        if (channel == null) return null;
        return HelpdeskTicket.TicketChannel.valueOf(channel.getValue());
    }

    default InteractionSenderType toDtoSenderType(TicketInteraction.SenderType senderType) {
        if (senderType == null) return null;
        return InteractionSenderType.valueOf(senderType.name());
    }

    default TicketInteraction.SenderType toEntitySenderType(InteractionSenderType senderType) {
        if (senderType == null) return null;
        return TicketInteraction.SenderType.valueOf(senderType.getValue());
    }

    default OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    default LocalDateTime offsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;
        return offsetDateTime.toLocalDateTime();
    }
}
