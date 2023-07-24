from enum import Enum


class VertexType(Enum):
    SYMBOLIC = 0
    VALUE = 1
    APP = 2


def get_vertex_type(name):
    if name == "SYMBOLIC":
        return VertexType.SYMBOLIC

    if name == "VALUE":
        return VertexType.VALUE

    return VertexType.APP


def process_line(line, v, operators, edges, depth):
    name, info = line.split(";")
    info = info.strip()
    vertex_type = get_vertex_type(name)
    depth.append(0)
    assert (len(depth) == v + 1)

    if vertex_type == VertexType.APP:
        operators.append(name)

        children = map(int, info.split(" "))
        for u in children:
            edges.append([u, v])
            depth[v] = max(depth[v], depth[u] + 1)
    else:
        operators.append(name + ";" + info)


def read_graph_from_file(path):
    operators, edges, depth = [], [], []

    with open(path, "r") as inf:
        v = 0
        for line in inf.readlines():
            line = line.strip()

            if line.startswith(";"):
                continue

            try:
                process_line(line, v, operators, edges, depth)
            except Exception as e:
                print(e, "\n")
                print(path, "\n")
                print(v, line, "\n")
                raise e

            v += 1

    return operators, edges, max(depth)
