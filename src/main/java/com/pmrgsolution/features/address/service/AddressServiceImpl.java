package com.pmrgsolution.features.address.service;

import com.pmrgsolution.Exception.ResourceNotFoundException;
import com.pmrgsolution.features.address.dto.AddressRequest;
import com.pmrgsolution.features.address.dto.AddressResponse;
import com.pmrgsolution.features.address.entity.ShippingAddress;
import com.pmrgsolution.features.address.repository.ShippingAddressRepository;
import com.pmrgsolution.features.auth.entity.User;
import com.pmrgsolution.features.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final ShippingAddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ShippingAddress> existing = addressRepository.findByUserId(userId);
        boolean isFirst = existing.isEmpty();
        boolean shouldBeDefault = isFirst || (request.getIsDefault() != null && request.getIsDefault());

        if (shouldBeDefault && !existing.isEmpty()) {
            existing.forEach(a -> a.setDefault(false));
            addressRepository.saveAll(existing);
        }

        ShippingAddress address = ShippingAddress.builder()
                .user(user)
                .fullName(request.getFullName().trim())
                .phoneNumber(request.getCleanPhoneNumber())
                .alternatePhone(request.getAlternatePhone() != null ? request.getAlternatePhone().trim() : null)
                .addressLine1(request.getAddressLine1().trim())
                .addressLine2(request.getAddressLine2() != null ? request.getAddressLine2().trim() : null)
                .city(request.getCity().trim())
                .state(request.getState().trim())
                .postalCode(request.getCleanPostalCode())
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry().trim() : "India")
                .addressType(request.getAddressType() != null && !request.getAddressType().isBlank() ? request.getAddressType().trim() : "Home")
                .isDefault(shouldBeDefault)
                .build();

        return mapToResponse(addressRepository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(UUID userId) {
        return addressRepository.findByUserId(userId).stream()
                .filter(a -> !"MANUAL_ORDER".equalsIgnoreCase(a.getAddressType()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(UUID userId, UUID addressId) {
        ShippingAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return mapToResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        ShippingAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (request.getIsDefault() != null && request.getIsDefault()) {
            List<ShippingAddress> all = addressRepository.findByUserId(userId);
            all.forEach(a -> a.setDefault(false));
            addressRepository.saveAll(all);
            address.setDefault(true);
        }

        address.setFullName(request.getFullName().trim());
        address.setPhoneNumber(request.getCleanPhoneNumber());
        address.setAlternatePhone(request.getAlternatePhone() != null ? request.getAlternatePhone().trim() : null);
        address.setAddressLine1(request.getAddressLine1().trim());
        address.setAddressLine2(request.getAddressLine2() != null ? request.getAddressLine2().trim() : null);
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setPostalCode(request.getCleanPostalCode());
        if (request.getCountry() != null && !request.getCountry().isBlank()) address.setCountry(request.getCountry().trim());
        if (request.getAddressType() != null && !request.getAddressType().isBlank()) address.setAddressType(request.getAddressType().trim());

        return mapToResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        ShippingAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        List<ShippingAddress> addresses = addressRepository.findByUserId(userId);
        ShippingAddress target = addresses.stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        addresses.forEach(a -> a.setDefault(a.getId().equals(addressId)));
        addressRepository.saveAll(addresses);
        return mapToResponse(target);
    }

    private AddressResponse mapToResponse(ShippingAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .alternatePhone(address.getAlternatePhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .addressType(address.getAddressType())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}