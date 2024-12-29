package serverutils.lib.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.util.Constants;

import serverutils.lib.io.ByteCounterOutputStream;

public class NBTUtils {

    public static final NBTTagByte BYTE_0 = new NBTTagByte((byte) 0);
    public static final NBTTagByte BYTE_1 = new NBTTagByte((byte) 1);

    @SuppressWarnings("ConstantConditions")
    public static void renameTag(NBTTagCompound nbt, String oldName, String newName) {
        NBTBase tag = nbt.getTag(oldName);

        if (tag != null) {
            nbt.removeTag(oldName);
            nbt.setTag(newName, tag);
        }
    }

    public static void writeNBT(File file, NBTTagCompound tag) {
        try (FileOutputStream stream = new FileOutputStream(FileUtils.newFile(file))) {
            CompressedStreamTools.writeCompressed(tag, stream);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void writeNBTSafe(File file, NBTTagCompound tag) {
        ThreadedFileIOBase.threadedIOInstance.queueIO(() -> {
            writeNBT(file, tag);
            return false;
        });
    }

    @Nullable
    public static NBTTagCompound readNBT(File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        try (InputStream stream = new FileInputStream(file)) {
            return CompressedStreamTools.readCompressed(stream);
        } catch (Exception ex) {
            try {
                return CompressedStreamTools.read(file);
            } catch (Exception ex1) {
                return null;
            }
        }
    }

    public static void copyTags(@Nullable NBTTagCompound from, @Nullable NBTTagCompound to) {
        if (from != null && to != null && !from.hasNoTags()) {
            for (String key : from.func_150296_c()) {
                to.setTag(key, from.getTag(key));
            }
        }
    }

    @Nullable
    public static NBTTagCompound minimize(@Nullable NBTTagCompound nbt) {
        if (nbt == null || nbt.hasNoTags()) {
            return null;
        }

        NBTTagCompound nbt1 = null;

        for (String key : nbt.func_150296_c()) {
            NBTBase nbt2 = nbt.getTag(key);

            if (nbt2 instanceof NBTTagCompound) {
                nbt2 = minimize((NBTTagCompound) nbt2);
            } else if (nbt2 instanceof NBTTagList) {
                nbt2 = minimize((NBTTagList) nbt2);
            }

            if (nbt2 != null) {
                if (nbt1 == null) {
                    nbt1 = new NBTTagCompound();
                }

                nbt1.setTag(key, nbt2);
            }
        }

        return nbt1;
    }

    @Nullable
    public static NBTTagList minimize(@Nullable NBTTagList nbt) {
        return nbt == null || nbt.tagCount() == 0 ? null : nbt;
    }

    private static final EnumChatFormatting[] COLORS = { EnumChatFormatting.BLUE, EnumChatFormatting.DARK_GREEN,
            EnumChatFormatting.YELLOW, EnumChatFormatting.RED };

    public static String getColoredNBTString(@Nullable NBTBase nbt) {
        return getColoredNBTString(new StringBuilder(), nbt, 0).toString();
    }

    private static StringBuilder getColoredNBTString(StringBuilder builder, @Nullable NBTBase nbt, int level) {
        if (nbt == null) {
            return builder.append(EnumChatFormatting.DARK_GRAY).append("null");
        }

        switch (nbt.getId()) {
            case Constants.NBT.TAG_END:
                return builder.append(EnumChatFormatting.DARK_GRAY).append("null");
            case Constants.NBT.TAG_BYTE:
            case Constants.NBT.TAG_SHORT:
            case Constants.NBT.TAG_INT:
            case Constants.NBT.TAG_LONG:
            case Constants.NBT.TAG_ANY_NUMERIC:
            case Constants.NBT.TAG_FLOAT:
            case Constants.NBT.TAG_DOUBLE:
                return builder.append(EnumChatFormatting.GRAY).append(nbt.toString());
            case Constants.NBT.TAG_STRING:
                return builder.append(EnumChatFormatting.GRAY).append(nbt.toString());
            case Constants.NBT.TAG_LIST: {
                NBTTagList list = (NBTTagList) nbt;
                builder.append(COLORS[level % COLORS.length]).append('[');

                for (int i = 0; i < list.tagCount(); i++) {
                    if (i > 0) {
                        builder.append(EnumChatFormatting.DARK_GRAY).append(',').append(' ');
                    }
                    getColoredNBTString(builder, (NBTBase) list.tagList.get(i), level + 1);
                }

                return builder.append(COLORS[level % COLORS.length]).append(']');
            }
            case Constants.NBT.TAG_COMPOUND: {
                NBTTagCompound map = (NBTTagCompound) nbt;
                builder.append(COLORS[level % COLORS.length]).append('{');

                boolean first = true;

                for (String key : map.func_150296_c()) {
                    if (first) {
                        first = false;
                    } else {
                        builder.append(EnumChatFormatting.DARK_GRAY).append(',').append(' ');
                    }

                    builder.append(EnumChatFormatting.DARK_GRAY).append(key).append(':').append(' ');
                    getColoredNBTString(builder, map.getTag(key), level + 1);
                }

                return builder.append(COLORS[level % COLORS.length]).append('}');
            }
            case Constants.NBT.TAG_BYTE_ARRAY: {
                NBTTagByteArray list = (NBTTagByteArray) nbt;
                builder.append(COLORS[level % COLORS.length]).append('[');

                for (int i = 0; i < list.func_150292_c().length; i++) {
                    if (i > 0) {
                        builder.append(EnumChatFormatting.DARK_GRAY).append(',').append(' ');
                    }

                    builder.append(EnumChatFormatting.GRAY).append(list.func_150292_c()[i]);
                }

                return builder.append(COLORS[level % COLORS.length]).append(']');
            }
            case Constants.NBT.TAG_INT_ARRAY: {
                NBTTagIntArray list = (NBTTagIntArray) nbt;
                builder.append(COLORS[level % COLORS.length]).append('[');

                for (int i = 0; i < list.func_150302_c().length; i++) {
                    if (i > 0) {
                        builder.append(EnumChatFormatting.DARK_GRAY).append(',').append(' ');
                    }

                    builder.append(EnumChatFormatting.GRAY).append(list.func_150302_c()[i]);
                }

                return builder.append(COLORS[level % COLORS.length]).append(']');
            }
            default:
                return builder.append(EnumChatFormatting.GRAY).append(nbt.toString());
        }
    }

    public static long getSizeInBytes(NBTTagCompound nbt, boolean compressed) {
        try {
            ByteCounterOutputStream byteCounter = new ByteCounterOutputStream();

            if (compressed) {
                CompressedStreamTools.writeCompressed(nbt, byteCounter);
            } else {
                CompressedStreamTools.write(nbt, new DataOutputStream(byteCounter));
            }

            return byteCounter.getSize();
        } catch (Exception ex) {
            return -1L;
        }
    }

    public static NBTTagCompound getPersistedData(EntityPlayer player, boolean createIfMissing) {
        NBTTagCompound tag = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        if (createIfMissing) {
            player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, tag);
        }

        return tag;
    }

    public static boolean getBooleanOrTrue(NBTTagCompound nbt, String key) {
        return !nbt.hasKey(key) || nbt.getBoolean(key);
    }
}
