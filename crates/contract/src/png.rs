//! A valid 1x1 RGBA PNG built in code, for the multipart image upload steps.

const SIGNATURE: [u8; 8] = [0x89, b'P', b'N', b'G', 0x0D, 0x0A, 0x1A, 0x0A];

/// Computes the CRC-32 (IEEE 802.3, reflected, as PNG uses it) without a table.
pub fn crc32(data: &[u8]) -> u32 {
    let mut crc = 0xFFFF_FFFFu32;
    for &byte in data {
        crc ^= u32::from(byte);
        for _ in 0..8 {
            let mask = (crc & 1).wrapping_neg();
            crc = (crc >> 1) ^ (0xEDB8_8320 & mask);
        }
    }
    !crc
}

/// Computes the Adler-32 checksum, the zlib trailer.
pub fn adler32(data: &[u8]) -> u32 {
    let (mut a, mut b) = (1u32, 0u32);
    for &byte in data {
        a = (a + u32::from(byte)) % 65521;
        b = (b + a) % 65521;
    }
    (b << 16) | a
}

fn chunk(kind: &[u8; 4], data: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(12 + data.len());
    out.extend_from_slice(&(data.len() as u32).to_be_bytes());
    let mut body = Vec::with_capacity(4 + data.len());
    body.extend_from_slice(kind);
    body.extend_from_slice(data);
    out.extend_from_slice(&body);
    out.extend_from_slice(&crc32(&body).to_be_bytes());
    out
}

/// Wraps `raw` in a zlib stream with one stored (uncompressed) block.
fn zlib_stored(raw: &[u8]) -> Vec<u8> {
    let len = raw.len() as u16;
    let mut out = vec![0x78, 0x01, 0x01];
    out.extend_from_slice(&len.to_le_bytes());
    out.extend_from_slice(&(!len).to_le_bytes());
    out.extend_from_slice(raw);
    out.extend_from_slice(&adler32(raw).to_be_bytes());
    out
}

/// Builds the upload PNG: 1x1, 8-bit RGBA, one opaque red pixel, 73 bytes.
pub fn tiny_png() -> Vec<u8> {
    let mut ihdr = Vec::with_capacity(13);
    ihdr.extend_from_slice(&1u32.to_be_bytes()); // width
    ihdr.extend_from_slice(&1u32.to_be_bytes()); // height
    ihdr.extend_from_slice(&[8, 6, 0, 0, 0]); // depth, RGBA, compression, filter, interlace
    let scanline = [0u8, 255, 0, 0, 255]; // filter none + RGBA
    let mut png = SIGNATURE.to_vec();
    png.extend(chunk(b"IHDR", &ihdr));
    png.extend(chunk(b"IDAT", &zlib_stored(&scanline)));
    png.extend(chunk(b"IEND", &[]));
    png
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn crc32_vector() {
        assert_eq!(crc32(b"123456789"), 0xCBF4_3926);
        assert_eq!(crc32(b""), 0);
    }

    #[test]
    fn adler32_vector() {
        assert_eq!(adler32(b"Wikipedia"), 0x11E6_0398);
    }

    #[test]
    fn png_is_well_formed() {
        let png = tiny_png();
        assert_eq!(png.len(), 73);
        assert_eq!(&png[..8], &SIGNATURE);
        assert_eq!(&png[12..16], b"IHDR");
        assert_eq!(&png[8..12], &13u32.to_be_bytes());
        assert_eq!(&png[png.len() - 8..png.len() - 4], b"IEND");
        // IEND's CRC is the CRC of its type alone.
        assert_eq!(&png[png.len() - 4..], &0xAE42_6082u32.to_be_bytes());
        // IHDR chunk CRC covers type + data.
        let ihdr_crc = u32::from_be_bytes([png[29], png[30], png[31], png[32]]);
        assert_eq!(ihdr_crc, crc32(&png[12..29]));
        // IDAT: zlib header, stored block, adler of the scanline.
        assert_eq!(&png[37..41], b"IDAT");
        assert_eq!(&png[41..43], &[0x78, 0x01]);
        let stream_len = u32::from_be_bytes([png[33], png[34], png[35], png[36]]) as usize;
        assert_eq!(stream_len, 16);
        let adler = u32::from_be_bytes([png[53], png[54], png[55], png[56]]);
        assert_eq!(adler, adler32(&[0, 255, 0, 0, 255]));
    }

    #[test]
    fn flate2_decodes_the_idat_stream() {
        use std::io::Read;
        let png = tiny_png();
        let mut raw = Vec::new();
        flate2::read::ZlibDecoder::new(&png[41..57])
            .read_to_end(&mut raw)
            .unwrap();
        assert_eq!(raw, vec![0, 255, 0, 0, 255]);
    }
}
