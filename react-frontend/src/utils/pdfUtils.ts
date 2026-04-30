/**
 * Counts the number of pages in a PDF file
 * @param file - The PDF file to count pages from
 * @returns Promise resolving to the number of pages
 */
export const countPDFPages = async (file: File): Promise<number> => {
  const arrayBuffer = await file.arrayBuffer();
  const text = new TextDecoder().decode(arrayBuffer);
  const rx = /\/Type\s*\/Page\b/g;
  let count = 0;
  while (rx.exec(text)) {
    count += 1;
  }
  return count || 1;
};

/**
 * Processes a file and adds page count metadata
 * @param file - The file to process
 * @returns Promise resolving to the file with page count
 */
export interface FileWithPages extends File {
  pageCount?: number;
}

export const processFileForPages = async (file: File): Promise<FileWithPages> => {
  const f: FileWithPages = file;
  if (file.type === "application/pdf") {
    try {
      f.pageCount = await countPDFPages(file);
    } catch (error) {
      f.pageCount = 1;
    }
  } else {
    f.pageCount = 1;
  }
  return f;
};