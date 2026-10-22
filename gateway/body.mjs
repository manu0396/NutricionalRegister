/**
 * The request body reader.
 *
 * The body's size is capped, so a client cannot grow the gateway's memory, and
 * a malformed envelope is rejected instead of being forwarded to the model.
 */

/** Reads the request body and parses it, or throws when it is not a sane JSON body. */
export function readBody(request, limitBytes) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    request.on("data", (chunk) => {
      size += chunk.length;
      if (size > limitBytes) {
        reject(new Error("too-large"));
        request.destroy();
        return;
      }
      chunks.push(chunk);
    });
    request.on("error", () => reject(new Error("aborted")));
    request.on("end", () => {
      const text = Buffer.concat(chunks).toString("utf8");
      if (!text) {
        reject(new Error("empty"));
        return;
      }
      try {
        resolve(JSON.parse(text));
      } catch {
        reject(new Error("not-json"));
      }
    });
  });
}
