using System.Text.Json.Serialization;
using Dotnet10Dapper.Dto;

namespace Dotnet10Dapper;

[JsonSerializable(typeof(FruitDto))]
[JsonSerializable(typeof(List<FruitDto>))]
[JsonSerializable(typeof(StoreFruitPriceDto))]
[JsonSerializable(typeof(StoreDto))]
[JsonSerializable(typeof(AddressDto))]
[JsonSourceGenerationOptions(
    PropertyNamingPolicy = JsonKnownNamingPolicy.CamelCase,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
public partial class FruitJsonContext : JsonSerializerContext;
