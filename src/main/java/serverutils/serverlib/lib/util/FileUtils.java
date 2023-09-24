package serverutils.serverlib.lib.util;

import net.minecraft.world.storage.ThreadedFileIOBase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileUtils
{
	public static final int KB = 1024;
	public static final int MB = KB * 1024;
	public static final int GB = MB * 1024;

	public static final double KB_D = 1024D;
	public static final double MB_D = KB_D * 1024D;
	public static final double GB_D = MB_D * 1024D;

	public static File newFile(File file)
	{
		if (!file.exists())
		{
			try
			{
				File parent = file.getParentFile();
				if (!parent.exists())
				{
					parent.mkdirs();
				}
				file.createNewFile();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		return file;
	}

	public static void save(File file, List<String> list) throws Exception
	{
		OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(newFile(file)), StandardCharsets.UTF_8);
		BufferedWriter br = new BufferedWriter(fw);

		for (String s : list)
		{
			br.write(s);
			br.write('\n');
		}

		br.close();
		fw.close();
	}

	public static void save(File file, String string) throws Exception
	{
		OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(newFile(file)), StandardCharsets.UTF_8);
		BufferedWriter br = new BufferedWriter(fw);
		br.write(string);
		br.close();
		fw.close();
	}

	public static void saveSafe(final File file, final List<String> list)
	{
		ThreadedFileIOBase.threadedIOInstance.queueIO(() ->
		{
			try
			{
				save(file, list);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}

			return false;
		});
	}

	public static void saveSafe(final File file, final String string)
	{
		ThreadedFileIOBase.threadedIOInstance.queueIO(() ->
		{
			try
			{
				save(file, string);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}

			return false;
		});
	}

	public static List<File> listTree(File file)
	{
		List<File> l = new ArrayList<>();
		listTree0(l, file);
		return l;
	}

	public static void listTree0(List<File> list, File file)
	{
		if (file.isDirectory())
		{
			File[] fl = file.listFiles();

			if (fl != null && fl.length > 0)
			{
				for (File aFl : fl)
				{
					listTree0(list, aFl);
				}
			}
		}
		else if (file.isFile())
		{
			list.add(file);
		}
	}

	public static long getSize(File file)
	{
		if (!file.exists())
		{
			return 0L;
		}
		else if (file.isFile())
		{
			return file.length();
		}
		else if (file.isDirectory())
		{
			long length = 0L;
			File[] f1 = file.listFiles();
			if (f1 != null && f1.length > 0)
			{
				for (File aF1 : f1)
				{
					length += getSize(aF1);
				}
			}
			return length;
		}
		return 0L;
	}

	public static String getSizeString(double b)
	{
		if (b >= GB_D)
		{
			return String.format("%.1fGB", b / GB_D);
		}
		else if (b >= MB_D)
		{
			return String.format("%.1fMB", b / MB_D);
		}
		else if (b >= KB_D)
		{
			return String.format("%.1fKB", b / KB_D);
		}

		return ((long) b) + "B";
	}

	public static String getSizeString(File file)
	{
		return getSizeString(getSize(file));
	}

	public static void copyFile(File src, File dst) throws Exception
	{
		if (src.exists() && !src.equals(dst))
		{
			if (src.isDirectory() && dst.isDirectory())
			{
				for (File f : listTree(src))
				{
					File dst1 = new File(dst.getAbsolutePath() + File.separatorChar + (f.getAbsolutePath().replace(src.getAbsolutePath(), "")));
					copyFile(f, dst1);
				}
			}
			else
			{
				dst = newFile(dst);

				try (FileInputStream fis = new FileInputStream(src);
					 FileOutputStream fos = new FileOutputStream(dst);
					 FileChannel srcC = fis.getChannel();
					 FileChannel dstC = fos.getChannel())
				{
					dstC.transferFrom(srcC, 0L, srcC.size());
				}
			}
		}
	}

	public static boolean delete(File file)
	{
		if (!file.exists())
		{
			return false;
		}
		else if (file.isFile())
		{
			return file.delete();
		}

		String[] files = file.list();

		if (files != null)
		{
			for (String s : files)
			{
				delete(new File(file, s));
			}
		}

		return file.delete();
	}

	public static void deleteSafe(File file)
	{
		ThreadedFileIOBase.threadedIOInstance.queueIO(() ->
		{
			try
			{
				if (file.exists() && !delete(file))
				{
					System.err.println("Failed to safely delete " + file.getAbsolutePath());
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}

			return false;
		});
	}

	public static String getBaseName(File file)
	{
		if (file.isDirectory())
		{
			return file.getName();
		}
		else
		{
			String name = file.getName();
			int index = name.lastIndexOf('.');
			return index == -1 ? name : name.substring(0, index);
		}
	}
}