package com.pmrgsolution.features.address.service;

import com.pmrgsolution.features.address.dto.AddressRequest;
import com.pmrgsolution.features.address.dto.AddressResponse;
import java.util.List;
import java.util.UUID;

public interface AddressService {
    AddressResponse addAddress(UUID userId, AddressRequest request);
    List<AddressResponse> getAddresses(UUID userId);
    AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request);
    void deleteAddress(UUID userId, UUID addressId);
    AddressResponse setDefaultAddress(UUID userId, UUID addressId);
    AddressResponse getAddressById(UUID userId, UUID addressId);
}