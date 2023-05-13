/******************************************************************************
 *
 *  Copyright (C) 2017 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#pragma once

#include <stdint.h>
#include <array>
#include <string>

namespace bluetooth {

// This class is representing Bluetooth UUIDs across whole stack.
// Here are some general endianness rules:
// 1. UUID is internally kept as as Big Endian.
// 2. Bytes representing UUID coming from upper layers, Java or Binder, are Big
//    Endian.
// 3. Bytes representing UUID coming from lower layer, HCI packets, are Little
//    Endian.
// 4. UUID in storage is always string.
class Uuid final {
 public:
  static constexpr size_t kNumBytes128 = 16;
  static constexpr size_t kNumBytes32 = 4;
  static constexpr size_t kNumBytes16 = 2;

  static constexpr size_t kString128BitLen = 36;

  static Uuid kEmpty;  // 00000000-0000-0000-0000-000000000000
  static Uuid kBase;   // 00000000-0000-1000-8000-00805f9b34fb

  using UUID128Bit = std::array<uint8_t, kNumBytes128>;

  // Creates and returns a random 128-bit UUID.
  static Uuid GetRandom();

  // Returns the shortest possible representation of this UUID in bytes. Either
  // kNumBytes16, kNumBytes32, or kNumBytes128
  size_t GetShortestRepresentationSize() const;

  // Returns true if this UUID can be represented as 16 bit.
  bool Is16Bit() const;

  // Returns 16 bit Little Endian representation of this UUID. Use
  // GetShortestRepresentationSize() or Is16Bit() before using this method.
  uint16_t As16Bit() const;

  // Returns 32 bit Little Endian representation of this UUID. Use
  // GetShortestRepresentationSize() before using this method.
  uint32_t As32Bit() const;

  // Converts string representing 128, 32, or 16 bit UUID in
  // xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx, xxxxxxxx, or xxxx format to UUID. If
  // set, optional is_valid parameter will be set to true if conversion is
  // successfull, false otherwise.
  static Uuid FromString(const std::string& uuid, bool* is_valid = nullptr);

  // Converts 16bit Little Endian representation of UUID to UUID
  static Uuid From16Bit(uint16_t uuid16bit);

  // Converts 32bit Little Endian representation of UUID to UUID
  static Uuid From32Bit(uint32_t uuid32bit);

  // Converts 128 bit Big Endian array representing UUID to UUID.
  static Uuid From128BitBE(const UUID128Bit& uuid);

  // Converts 128 bit Big Endian array representing UUID to UUID. |uuid| points
  // to beginning of array.
  static Uuid From128BitBE(const uint8_t* uuid);

  // Converts 128 bit Little Endian array representing UUID to UUID.
  static Uuid From128BitLE(const UUID128Bit& uuid);

  // Converts 128 bit Little Endian array representing UUID to UUID. |uuid|
  // points to beginning of array.
  static Uuid From128BitLE(const uint8_t* uuid);

  // Returns 128 bit Little Endian representation of this UUID
  const UUID128Bit To128BitLE() const;

  // Returns 128 bit Big Endian representation of this UUID
  const UUID128Bit& To128BitBE() const;

  // Returns string representing this UUID in
  // xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx format, lowercase.
  std::string ToString() const;

  // Returns true if this UUID is equal to kEmpty
  bool IsEmpty() const;

  bool operator<(const Uuid& rhs) const;
  bool operator==(const Uuid& rhs) const;
  bool operator!=(const Uuid& rhs) const;

 private:
  // Network-byte-ordered ID (Big Endian).
  UUID128Bit uu;
};
}  // namespace bluetooth

inline std::ostream& operator<<(std::ostream& os, const bluetooth::Uuid& a) {
  os << a.ToString();
  return os;
}

// Custom std::hash specialization so that bluetooth::UUID can be used as a key
// in std::unordered_map.
namespace std {

template <>
struct hash<bluetooth::Uuid> {
  std::size_t operator()(const bluetooth::Uuid& key) const {
    const auto& uuid_bytes = key.To128BitBE();
    std::hash<std::string> hash_fn;
    return hash_fn(std::string(reinterpret_cast<const char*>(uuid_bytes.data()),
                               uuid_bytes.size()));
  }
};

}  // namespace std
