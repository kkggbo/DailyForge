import {
  getSystemExerciseDetail,
  getSystemExerciseFilterOptions,
  searchSystemExercises
} from "./exercise";

describe("exercise api client", () => {
  it("requests filter options with authorization", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(
        new Response(
          JSON.stringify({
            code: "SUCCESS",
            message: "ok",
            data: {
              categories: []
            }
          }),
          { status: 200, headers: { "Content-Type": "application/json" } }
        )
      );

    await getSystemExerciseFilterOptions("token-1");

    const [input, init] = fetchSpy.mock.calls[0] as [string, RequestInit];
    expect(input).toBe("/api/exercises/system/filter-options");
    expect(init.headers).toMatchObject({
      Authorization: "Bearer token-1"
    });
  });

  it("trims keyword and applies default pagination for search", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(
        new Response(
          JSON.stringify({
            code: "SUCCESS",
            message: "ok",
            data: {
              page: 1,
              pageSize: 20,
              total: 0,
              records: []
            }
          }),
          { status: 200, headers: { "Content-Type": "application/json" } }
        )
      );

    await searchSystemExercises("token-2", {
      keyword: "  bench  ",
      categoryCode: "chest",
      muscleId: 11
    });

    const [input] = fetchSpy.mock.calls[0] as [string];
    const url = new URL(input, "http://localhost");
    expect(url.pathname).toBe("/api/exercises/system");
    expect(url.searchParams.get("keyword")).toBe("bench");
    expect(url.searchParams.get("categoryCode")).toBe("chest");
    expect(url.searchParams.get("muscleId")).toBe("11");
    expect(url.searchParams.get("page")).toBe("1");
    expect(url.searchParams.get("pageSize")).toBe("20");
  });

  it("requests exercise detail by id", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(
        new Response(
          JSON.stringify({
            code: "SUCCESS",
            message: "ok",
            data: {
              exerciseId: 99
            }
          }),
          { status: 200, headers: { "Content-Type": "application/json" } }
        )
      );

    await getSystemExerciseDetail("token-3", 99);

    const [input] = fetchSpy.mock.calls[0] as [string];
    expect(input).toBe("/api/exercises/system/99");
  });
});
