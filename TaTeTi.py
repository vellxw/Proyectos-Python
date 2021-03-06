# Crea un tablero de 3x3 vacio (3 filas y 3 columnas)
tablero = [[' ', ' ', ' '], [' ', ' ', ' '], [' ', ' ', ' ']]


def tablero_inicial():
    # Imprime una lista con las posiciones de las columnas
    print(' '*3 + '0 - 1 - 2')
    print('0' + ' ' * 2 + tablero[0][0] + ' | ' + tablero[0]
          [1] + ' | ' + tablero[0][2] + ' ')  # Imprime la primera fila
    print(' ' * 2 + '---+---+---')  # Decora el tablero con lineas
    print('1' + ' ' * 2 + tablero[1][0] + ' | ' + tablero[1]
          [1] + ' | ' + tablero[1][2] + ' ')  # Imprime la segunda fila
    print(' ' * 2 + '---+---+---')  # Decora el tablero con lineas
    print('2' + ' ' * 2 + tablero[2][0] + ' | ' + tablero[2]
          [1] + ' | ' + tablero[2][2] + ' ')  # Imprime la tercera fila


def validar_ganador():
    global resultado
    resultado = 0
    if tablero[0][0] == 'X' and tablero[0][1] == 'X' and tablero[0][2] == 'X':
        resultado = 3
    elif tablero[1][0] == 'X' and tablero[1][1] == 'X' and tablero[1][2] == 'X':
        resultado = 3
    elif tablero[2][0] == 'X' and tablero[2][1] == 'X' and tablero[2][2] == 'X':
        resultado = 3
    elif tablero[0][0] == 'X' and tablero[1][0] == 'X' and tablero[2][0] == 'X':
        resultado = 3
    elif tablero[0][1] == 'X' and tablero[1][1] == 'X' and tablero[2][1] == 'X':
        resultado = 3
    elif tablero[0][2] == 'X' and tablero[1][2] == 'X' and tablero[2][2] == 'X':
        resultado = 3
    elif tablero[0][0] == 'X' and tablero[1][1] == 'X' and tablero[2][2] == 'X':
        resultado = 3
    elif tablero[0][2] == 'X' and tablero[1][1] == 'X' and tablero[2][0] == 'X':
        resultado = 3
    elif tablero[0][0] == 'O' and tablero[0][1] == 'O' and tablero[0][2] == 'O':
        resultado = 4
    elif tablero[1][0] == 'O' and tablero[1][1] == 'O' and tablero[1][2] == 'O':
        resultado = 4
    elif tablero[2][0] == 'O' and tablero[2][1] == 'O' and tablero[2][2] == 'O':
        resultado = 4
    elif tablero[0][0] == 'O' and tablero[1][0] == 'O' and tablero[2][0] == 'O':
        resultado = 4
    elif tablero[0][1] == 'O' and tablero[1][1] == 'O' and tablero[2][1] == 'O':
        resultado = 4
    elif tablero[0][2] == 'O' and tablero[1][2] == 'O' and tablero[2][2] == 'O':
        resultado = 4
    elif tablero[0][0] == 'O' and tablero[1][1] == 'O' and tablero[2][2] == 'O':
        resultado = 4
    elif tablero[0][2] == 'O' and tablero[1][1] == 'O' and tablero[2][0] == 'O':
        resultado = 4
    elif tablero[0][0] != ' ' and tablero[0][1] != ' ' and tablero[0][2] != ' ' and tablero[1][0] != ' ' and tablero[1][1] != ' ' and tablero[1][2] != ' ' and tablero[2][0] != ' ' and tablero[2][1] != ' ' and tablero[2][2] != ' ':
        resultado = 'Empate'


def Elecci??n():
    global x
    global o
    jugador1 = input(str(nombre1) + ' ' + '??Qu?? quieres ser? (X|O)')
    if jugador1 == 'X' or jugador1 == 'x':
        print(nombre1, 'es "X" y', nombre2, 'es "O"')
        print(nombre1, 'tiene el primer turno')
        x = nombre1
        o = nombre2
    elif jugador1 == 'O' or jugador1 == 'o':
        print(nombre1, 'es "O" y', nombre2, 'es "X"')
        print(nombre2, 'tiene el primer turno')
        x = nombre2
        o = nombre1
    else:
        print('Respuesta incorrecta, intente de nuevo')
        Elecci??n()


def Elecci??n_de_posici??n_x():
    posici??n = input(str(x) + ' ' + '??Cu??l es tu posici??n X? (0,2):')

    if posici??n == '0-0':
        if tablero[0][0] == ' ':
            tablero[0][0] = 'X'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_x()
    elif posici??n == '0-1':
        if tablero[0][1] == ' ':
            tablero[0][1] = 'X'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_x()
    elif posici??n == '0-2':
        if tablero[0][2]:
            tablero[0][2] = 'X'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_x()
    elif posici??n == '1-0':
        if tablero[1][0] == ' ':
            tablero[1][0] = 'X'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_x()
    elif posici??n == '1-1':
        if tablero[1][1] == ' ':
            tablero[1][1] = 'X'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_x()
    elif posici??n == '1-2':
        if tablero[1][2] == ' ':
            tablero[1][2] = 'X'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_x()
    elif posici??n == '2-0':
        if tablero[2][0] == ' ':
            tablero[2][0] = 'X'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_x()
    elif posici??n == '2-1':
        if tablero[2][1] == ' ':
            tablero[2][1] = 'X'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_x()
    elif posici??n == '2-2':
        if tablero[2][2] == ' ':
            tablero[2][2] = 'X'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_x()
    else:
        print('Respuesta incorrecta, intente de nuevo')
        Elecci??n_de_posici??n_x()


def Elecci??n_de_posici??n_o():
    posici??n = input(str(o) + ' ' + '??Cu??l es tu posici??n O? (0-2):')

    if posici??n == '0-0':
        if tablero[0][0] == ' ':
            tablero[0][0] = 'O'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_o()
    elif posici??n == '0-1':
        if tablero[0][1] == ' ':
            tablero[0][1] = 'O'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_o()
    elif posici??n == '0-2':
        if tablero[0][2] == ' ':
            tablero[0][2] = 'O'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_o()
    elif posici??n == '1-0':
        if tablero[1][0] == ' ':
            tablero[1][0] = 'O'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_o()
    elif posici??n == '1-1':
        if tablero[1][1] == ' ':
            tablero[1][1] = 'O'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_o()
    elif posici??n == '1-2':
        if tablero[1][2] == ' ':
            tablero[1][2] = 'O'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_o()
    elif posici??n == '2-0':
        if tablero[2][0] == ' ':
            tablero[2][0] = 'O'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_o()
    elif posici??n == '2-1':
        if tablero[2][1] == ' ':
            tablero[2][1] = 'O'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_o()
    elif posici??n == '2-2':
        if tablero[2][2] == ' ':
            tablero[2][2] = 'O'
            tablero_inicial()
        else:
            print('Posici??n ocupada, intente de nuevo')
            Elecci??n_de_posici??n_o()
    else:
        print('Respuesta incorrecta, intente de nuevo')
        Elecci??n_de_posici??n_o()


def ENTER():
    enter = input('Presione ENTER para continuar')
    if enter == '':
        print(' ')
        print('| Este va a ser su tablero de juego |')
        tablero_inicial()
    else:
        print('No se ha presionado ENTER')
        ENTER()


print('| Bienvenido a Ta-Te-Ti (Tic-Tac-Toe) en Python |')

nombre1 = input('Ingrese nombre del jugador 1: ')
nombre2 = input('Ingrese nombre del jugador 2: ')

Elecci??n()
ENTER()

print('Los n??meros que est??n arriba son las posiciones de las columnas')
print('Los n??meros que est??n a la izquierda son las posiciones de las filas')
print('las columnas y las filas se numeran desde el 0 hasta el 2 (0, 1, 2)')
print('la posici??n que queremos se escribe de la siguiente manera: 0-1 (primero la fila, luego la columna)')
print('eso dejar?? en la posici??n (0, 1), o sea primera fila "0" y segunda columna al medio "1" al "X"')
print(' ')
print('|' + ' ' + str(x) + ',' + ' ' + 'comienza tu turno con la X' +
      ' ' + '|')  # Imprime el nombre del jugador 1 con la X


while True:
    Elecci??n_de_posici??n_x()
    validar_ganador()
    if resultado == 3:
        print('|' + ' ' + str(x) + ' ' + 'ganaste con las X' + ' ' + '|')
        break
    elif resultado == 4:
        print('|' + ' ' + str(o) + ' ' + 'ganaste con las O' + ' ' + '|')
        break
    elif resultado == 'Empate':
        print('|' + ' ' + 'Empate' + ' ' + '|')
        break
    Elecci??n_de_posici??n_o()
    validar_ganador()
    if resultado == 3:
        print('|' + ' ' + str(x) + ' ' + 'ganaste con las X' + ' ' + '|')
        break
    elif resultado == 4:
        print('|' + ' ' + str(o) + ' ' + 'ganaste con las O' + ' ' + '|')
        break
    elif resultado == 'Empate':
        print('|' + ' ' + 'Empate' + ' ' + '|')
        break
