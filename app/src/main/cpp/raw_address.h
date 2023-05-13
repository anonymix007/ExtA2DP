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

#include <string>

/** Bluetooth Address */
class RawAddress final {
 public:
  static constexpr unsigned int kLength = 6;

  uint8_t address[kLength];

  RawAddress() = default;
  RawAddress(const uint8_t (&addr)[6]);

  bool operator<(const RawAddress& rhs) const {
    return (std::memcmp(address, rhs.address, sizeof(address)) < 0);
  }
  bool operator==(const RawAddress& rhs) const {
    return (std::memcmp(address, rhs.address, sizeof(address)) == 0);
  }
  bool operator>(const RawAddress& rhs) const { return (rhs < *this); }
  bool operator<=(const RawAddress& rhs) const { return !(*this > rhs); }
  bool operator>=(const RawAddress& rhs) const { return !(*this < rhs); }
  bool operator!=(const RawAddress& rhs) const { return !(*this == rhs); }

  bool IsEmpty() const { return *this == kEmpty; }

  std::string ToString() const;

  // Converts |string| to RawAddress and places it in |to|. If |from| does
  // not represent a Bluetooth address, |to| is not modified and this function
  // returns false. Otherwise, it returns true.
  static bool FromString(const std::string& from, RawAddress& to);

  static bool IsValidAddress(const std::string& address);

  static const RawAddress kEmpty;  // 00:00:00:00:00:00
  static const RawAddress kAny;    // FF:FF:FF:FF:FF:FF
};

inline std::ostream& operator<<(std::ostream& os, const RawAddress& a) {
  os << a.ToString();
  return os;
}
