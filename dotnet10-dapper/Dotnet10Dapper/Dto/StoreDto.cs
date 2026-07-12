namespace Dotnet10Dapper.Dto;

public record StoreDto(long? Id, string Name, string Currency, AddressDto Address);
