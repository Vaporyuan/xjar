package io.xjar;

import io.xjar.key.XKey;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.Deflater;

/**
 * ZIP压缩包加密器
 *
 * @author Payne 646742615@qq.com
 * 2018/11/22 15:27
 */
public class XZipEncryptor extends XEntryEncryptor<ZipArchiveEntry> implements XEncryptor {
    private final int level;

    public XZipEncryptor(XEncryptor xEncryptor) {
        this(xEncryptor, (Collection<XEntryFilter<ZipArchiveEntry>>) null);
    }

    public XZipEncryptor(XEncryptor xEncryptor, XEntryFilter<ZipArchiveEntry>... filters) {
        this(xEncryptor, Arrays.asList(filters));
    }

    public XZipEncryptor(XEncryptor xEncryptor, Collection<XEntryFilter<ZipArchiveEntry>> filters) {
        this(xEncryptor, Deflater.DEFLATED, filters);
    }

    public XZipEncryptor(XEncryptor xEncryptor, int level) {
        this(xEncryptor, level, (Collection<XEntryFilter<ZipArchiveEntry>>) null);
    }

    public XZipEncryptor(XEncryptor xEncryptor, int level, XEntryFilter<ZipArchiveEntry>... filters) {
        this(xEncryptor, Arrays.asList(filters));
    }

    public XZipEncryptor(XEncryptor xEncryptor, int level, Collection<XEntryFilter<ZipArchiveEntry>> filters) {
        super(xEncryptor, filters);
        this.level = level;
    }

    @Override
    public void encrypt(XKey key, File src, File dest) throws IOException {
        try (
                FileInputStream fis = new FileInputStream(src);
                FileOutputStream fos = new FileOutputStream(dest)
        ) {
            encrypt(key, fis, fos);
        }
    }

    @Override
    public void encrypt(XKey key, InputStream in, OutputStream out) throws IOException {
        ZipArchiveInputStream zis = null;
        ZipArchiveOutputStream zos = null;
        try {
            zis = new ZipArchiveInputStream(in);
            zos = new ZipArchiveOutputStream(out);
            zos.setLevel(level);
            NoCloseOutputStream nos = new NoCloseOutputStream(zos);
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                zos.putArchiveEntry(new ZipArchiveEntry(entry.getName()));
                XEncryptor encryptor = filter(entry) ? this : xNopEncryptor;
                try (OutputStream eos = encryptor.encrypt(key, nos)) {
                    XKit.transfer(zis, eos);
                }
                zos.closeArchiveEntry();
            }
            zos.finish();
        } finally {
            XKit.close(zis);
            XKit.close(zos);
        }
    }
}
