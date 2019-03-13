package com.inefficiently.epubmaker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WebCrawler {
	int CrawlDelay = 100;
	String AuthorName = "";
	String StoryName = "";
	String Directory = "";
	String Separator = "";
	int StoryDataBaseLine = 0;
	int ChapterBaseLine = 0;
	int StoryID;
	String[] Chapters;
	public WebCrawler(int StoryID) {
	this.StoryID = StoryID;	
	}
	public static void main(String[] args) {
		if(args.length == 1) {
			try {
				WebCrawler crawl = new WebCrawler(Integer.parseInt(args[0]));
				crawl.init();
				crawl.run();
			} catch(NumberFormatException e) {
				System.out.println("Invalid Story ID");
			}
		} else {
			try {
				Scanner scan = new Scanner(System.in);
				System.out.print("Please enter the fiction press story id:");
				int id = scan.nextInt();
				scan.close();
				WebCrawler crawl = new WebCrawler(id);
				crawl.init();
				crawl.run();
			} catch(NumberFormatException e) {
				System.out.println("Invalid Story ID");
			}
		}
	}
	public void init() {
		String Html = GrabHtml(1);
		SetDirectory();
		setStoryDataBaseLine(Html);
		setChapterBaseLine(Html);
		System.out.print("Getting:");
		SetStoryName(Html);
		System.out.print("By:");
		SetAuthorName(Html);
		SetChapters(Html);
		System.out.println("Found " + Chapters.length + " chapters to get");
	}
	public void run() {
		MakeDirectory();
		MakeTitlePage();
		MakeOpf();
		MakeTOCNCX();
		MakeAllChapters();
		System.out.println("Done getting Chapters!");
		MakeEpub();
		DeleteDirectory();
	}
	public void SetChapters(String Html) {
		String Options = GetHtmlLine(Html, ChapterBaseLine + 10);
		this.Chapters = ParseChapters(Options);
	}
	public String[] ParseChapters(String Options) {
		String cut = Options.substring(Options.indexOf("option  value=1 selected>") + "option  value=1 selected>".length(), Options.indexOf("</select"));
		String[] splitOptions = cut.split(Pattern.quote(" >"));
		for (int i = 0; i < splitOptions.length; i++) {
			if(i != splitOptions.length - 1)
				splitOptions[i] = splitOptions[i].substring(0, splitOptions[i].indexOf("<option")).trim();
			splitOptions[i] = "(Chapter " + (i + 1) + ")" + splitOptions[i].substring(splitOptions[i].indexOf(" "));
		}
		return splitOptions;
	}

	public void MakeChapterHTML(String Html, String Name) {
		MakeFile(Html, Name, "html");
	}
	public void MakeFile(String Data, String Name, String Extension) {
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Directory + Separator + StoryName + Separator + Name + "." + Extension), "UTF-8"));
			out.write(Data);
			out.close();
		} catch(IOException e) {
			System.out.println("Unable to write the " + Extension + " file for " + Name);
		}
	}
	public String GrabHtml(int ChapterNumber) {
		URL url;
		String Html = "";
		try {
			url = new URL("https://www.fictionpress.com/s/" + StoryID + "/" + ChapterNumber + "/");
			Scanner scan = new Scanner(url.openStream(), "UTF-8");
			while(scan.hasNextLine()) {
				Html += scan.nextLine() + "\n";
			}
			scan.close();
		} catch (IOException e) {
			System.out.println("Unable to get the book with the story id " + StoryID + ".");
		}
		return Html;
	}
	public String GetStory(String Html) {
		setChapterBaseLine(Html);
		String Line = GetHtmlLine(Html, ChapterBaseLine + 8);
		Line = Line.substring(Line.indexOf("<div class=\"storytext xcontrast_txt nocopy\" id=\"storytext\">") + "<div class=\"storytext xcontrast_txt nocopy\" id=\"storytext\">".length() + 1);
		return Line;
	}

	public String GetChapterTitle(int chapter) {
		return Chapters[chapter].substring(Chapters[chapter].indexOf(") ") + 2);
	}

	public void SetStoryName(String Html) {
		String Line = GetHtmlLine(Html, StoryDataBaseLine + 1);
		Line = Line.substring(Line.indexOf("<b class='xcontrast_txt'>") + "<b class='xcontrast_txt'>".length(), Line.indexOf("</b>"));
		StoryName = Line;
		System.out.println(StoryName);
	}

	public void SetAuthorName(String Html) {
		String Line = GetHtmlLine(Html, StoryDataBaseLine + 2);
		Line = Line.substring(Line.indexOf("<a class='xcontrast_txt'") + "<a class='xcontrast_txt'".length(), Line.indexOf("</a>"));	
		Line = Line.substring(Line.indexOf(">") + 1);
		AuthorName = Line;
		System.out.println(AuthorName);
	}
	public void SetDirectory() {
		try {
			File f = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
			Directory = f.getAbsolutePath().substring(0,f.getAbsolutePath().indexOf(f.getName()));
			Separator = System.getProperty("file.separator");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	public String GetHtmlLine(String Html, int LineNumber) {
		Scanner scan = new Scanner(Html);
		for (int i = 0; i < LineNumber - 1; i++) {
			scan.nextLine();
		}
		String Line = scan.nextLine();
		scan.close();
		return Line;
	}

	public void setStoryDataBaseLine(String Html) {
		int i = 1;
		Scanner scan = new Scanner(Html);
		String search = "function toggleTheme() {";
		while(scan.hasNextLine()) {
			if(scan.nextLine().equals(search)) {
				i += 7;
				break;
			} else {
				i++;
			}
		}
		scan.close();
		StoryDataBaseLine = i;
	}

	public void setChapterBaseLine(String Html) {
		int i = 1;
		Scanner scan = new Scanner(Html);
		String search = "document.write('<style> .storytext { max-height: 999999px; width: '+XCOOKIE.read_width+'%; font-size:' + XCOOKIE.read_font_size + 'em; font-family: \"'+XCOOKIE.read_font+'\"; line-height: '+XCOOKIE.read_line_height+'; text-align: left;} </style>');";
		while(scan.hasNextLine()) {
			if(scan.nextLine().equals(search)) {
				break;
			} else {
				i++;
			}
		}
		scan.close();
		ChapterBaseLine = i;
	}
	public void MakeTitlePage() {
		String Text = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Title Page</title></head><body style=\"text-align:center\"><h1>" + StoryName + "</h1><br><h2>By: " + AuthorName + "</h2><br><h3><a href=\"https://www.fictionpress.com/s/" + StoryID + "/1/\">Source</a></h3><br><p>Please support the author.</p><br><p>Thank you for using the epub maker by Inefficiently.</p><p>If you found this program useful, could kindly visit the <a href=\"https://github.com/Inefficiently/FictionPressEpubMaker\">github page</a> and star the repository that would be great!</p></body></html>";
		MakeChapterHTML(Text, "Title");
	}

	public void MakeDirectory() {
		File f = new File(Directory + Separator + StoryName);
		f.mkdir();
	}
	public void DeleteDirectory() {
		File directory = new File(Directory + StoryName);
		List<File> fileList = new ArrayList<File>();
		getAllFiles(directory, fileList);
		for (Iterator<File> iterator = fileList.iterator(); iterator.hasNext();) {
			File file = (File) iterator.next();
			file.delete();
		}
		directory.delete();
	}
	public void MakeOpf() {
		SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
		String date = format.format(new Date());
		String Data = "<?xml version='1.0' encoding='utf-8'?><package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" xml:lang=\"en\" unique-identifier=\"" + StoryID + "\" prefix=\"https://www.fictionpress.com/s/\"><metadata xmlns:dc=\"http://purl.org/dc/elements/1.1\"><dc:title id=\"title\">" + StoryName + "</dc:title><dc:date>" + date + "</dc:date><dc:creator id=\"creator\">" + AuthorName + "</dc:creator><dc:identifier id=\"" + StoryID + "\">fictionpress.com-" + replaceAll(StoryName," ","-") + "</dc:identifier><dc:language>en-US</dc:language></metadata><manifest>\r\n\t<item href=\"Title.html\" id=\"id\" media-type=\"application/xhtml+xml\"/>\r\n";
		for (int i = 0; i < Chapters.length; i++) {
			Data += "\t<item href=\"" + replaceAll(Chapters[i], " ", "%20") + ".html\" id=\"id" + (i+1) + "\" media-type=\"application/xhtml+xml\"/>\r\n";	
		}
		Data += "<item href=\"toc.ncx\" id=\"ncx\" media-type=\"application/x-dtbncx+xml\"/></manifest><spine toc=\"ncx\">\r\n\t<itemref idref=\"id\"/>\r\n";
		for (int i = 0; i < Chapters.length; i++) {
			Data += "\t<itemref idref=\"id" + (i+1) + "\"/>\r\n";
		}
		Data += "</spine><guide/></package>";
		MakeFile(Data, "metadata", "opf");
	}
	public void MakeTOCNCX() {
		String Data = "<?xml version='1.0' encoding='utf-8'?><ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\" xml:lang=\"en\"><head><meta content=\"2\" name=\"dtb:depth\"/><meta content=\"0\" name=\"dtb:totalPageCount\"/><meta content=\"0\" name=\"dtb:maxPageNumber\"/></head><docTitle><text>" + StoryName + "</text></docTitle><navMap>\r\n\t<navPoint id=\"num_1\" playOrder=\"1\"><navLabel><text>Title</text></navLabel><content src=\"Title.html\"/></navPoint>\r\n";
		for (int i = 0; i < Chapters.length; i++) {
			Data += "\t<navPoint id=\"num_"+ (i+2) + "\" playOrder=\"" + (i+2) + "\"><navLabel><text>" + Chapters[i] + "</text></navLabel><content src=\"" + replaceAll(Chapters[i], " ", "%20") + ".html\"/></navPoint>\r\n";
		}
		Data += "</navMap></ncx>";
		MakeFile(Data, "toc", "ncx");
	}

	public String replaceAll(String input, String search, String replacement) {
		String copy = input;
		return copy.replaceAll(Pattern.quote(search), replacement);
	}
	public void MakeAllChapters() {
		String Data;
		for (int i = 0; i < Chapters.length; i++) {

			Data = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>" + Chapters[i] + "</title></head><body style=\"text-align:center\"><h1>" + Chapters[i].substring(Chapters[i].indexOf(") ") + 2) + "</h1>";
			Data += GetStory(GrabHtml(i + 1));
			Data += "</body></html>";
			MakeFile(Data, Chapters[i], "html");
			System.out.println("Got Chapter " + (i+1));
			try {
				TimeUnit.MILLISECONDS.sleep(CrawlDelay);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/*
	 *  The following code was re-purposed to make epubs
	 *  The orginal code can be found at
	 *  http://www.avajava.com/tutorials/lessons/how-do-i-zip-a-directory-and-all-its-contents.html
	 *  This code was very helpful in making this project possible 
	 */
	public void MakeEpub() {
		File directoryToZip = new File(Directory + StoryName);
		List<File> fileList = new ArrayList<File>();
		getAllFiles(directoryToZip, fileList);
		System.out.println("---Creating Epub file");
		writeZipFile(directoryToZip, fileList);
		System.out.println("---Done");
	}
	public void getAllFiles(File dir, List<File> fileList) {
		File[] files = dir.listFiles();
		for (File file : files) {
			fileList.add(file);
			if (file.isDirectory()) {
				getAllFiles(file, fileList);
			}
		}
	}
	public void writeZipFile(File directoryToZip, List<File> fileList) {
		try {
			FileOutputStream fos = new FileOutputStream(Directory + Separator + StoryName + ".epub");
			ZipOutputStream zos = new ZipOutputStream(fos);
			for (File file : fileList) {
				if (!file.isDirectory()) {
					addToZip(directoryToZip, file, zos);
				}
			}
			zos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws FileNotFoundException,
	IOException {
		FileInputStream fis = new FileInputStream(file);
		String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,file.getCanonicalPath().length());
		System.out.println("Writing '" + zipFilePath + "' to epub file");
		ZipEntry zipEntry = new ZipEntry(zipFilePath);
		zos.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}
		zos.closeEntry();
		fis.close();
	}
}
