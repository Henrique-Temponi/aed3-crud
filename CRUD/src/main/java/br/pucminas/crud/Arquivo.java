package br.pucminas.crud;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class Arquivo<T extends Registro>
{
    /**
     * Nome do arquivo
     */
    public String nomeArquivo;

    /**
     * Objeto arquivo
     */
    public RandomAccessFile arquivo;

    /**
     * Construtor da classe genérica
     */
    private Constructor<T> construtor;

    /**
     * Tamanho em bytes do cabeçalho do arquivo
     */
    private final int HEADER_SIZE = 4;
    
    /**
     * Cria arquivo de dados para a entidaded
     * @param _construtor Construtor da classe da entidade
     * @param _nomeArquivo Nome do arquivo
     * @throws Exception
     */
    public Arquivo(Constructor<T> _construtor, String _nomeArquivo) throws Exception
    {
        construtor = _construtor;
        nomeArquivo = _nomeArquivo;

        File d = new File("dados"); // Diretório para o arquivo

        if(!d.exists())
            d.mkdir();

        arquivo = new RandomAccessFile("dados/" + nomeArquivo, "rw"); // Abrir o arquivo

        // Se o arquivo for menor do que o tamanho do cabeçalho, logo não possuir cabeçalho
        // Escreve 0 para representar o último ID utilizado
        if(arquivo.length() < HEADER_SIZE)
            arquivo.writeInt(0);        
    }
    
    /**
     * Inclui um registro
     * @param _obj Registro
     * @return ID do um registro
     * @throws Exception
     */
    public int incluir(T _obj) throws Exception
    {
        return incluir(_obj, 0, arquivo.length());
    }

    /**
     * Inclui um registro
     * @param _obj Registro
     * @param _lixo Tamanho do lixo após o registro
     * @param _pos Posição a ser escrita
     * @return ID do um registro
     * @throws Exception
     */
    private int incluir(T _obj, int _lixo, long _pos) throws Exception
    {
        if (_obj.getID() <= 0)
        {
            arquivo.seek(0);
            
            int ultimoID = arquivo.readInt() + 1;
            _obj.setID(ultimoID);
            
            arquivo.seek(0);
            arquivo.writeInt(ultimoID);
        }
        
        arquivo.seek(_pos);

        arquivo.writeByte(' ');
        byte[] byteArray = _obj.toByteArray();

        arquivo.writeInt(byteArray.length); // Tamanho do registro
        arquivo.write(byteArray);

        arquivo.writeInt(_lixo);

        return _obj.getID();
    }
    
    // Método apenas para testes, pois geralmente a memória principal raramente
    // será suficiente para manter todos os registros simultaneamente
    /**
     * Lista os registros do arquivo
     * @return Array com os registros
     * @throws Exception
     */
    public Object[] listar() throws Exception
    {
        ArrayList<T> lista = new ArrayList<>();

        arquivo.seek(HEADER_SIZE);
        
        byte lapide;
        byte[] byteArray;
        int size;
        T obj;
        
        while(arquivo.getFilePointer() < arquivo.length())
        {
            obj = construtor.newInstance();
            lapide = arquivo.readByte();
            size = arquivo.readInt();
        
            byteArray = new byte[size];
        
            arquivo.read(byteArray);
        
            if(lapide == ' ')
            {
                obj.fromByteArray(byteArray);
                lista.add(obj);
            }

            arquivo.skipBytes(arquivo.readInt()); // Pular o lixo
        }
        
        return lista.toArray();
    }
    
    /**
     * Encontra um registro
     * @param _id ID do registro
     * @return Objeto genérico com os dados do registro
     * @throws Exception
     */
    public Object buscar(int _id) throws Exception
    {
        arquivo.seek(HEADER_SIZE);

        byte lapide;
        byte[] byteArray;
        int size;
        T obj = null;

        while(arquivo.getFilePointer() < arquivo.length())
        {
            obj = construtor.newInstance();
            lapide = arquivo.readByte();
            size = arquivo.readInt();

            byteArray = new byte[size];

            arquivo.read(byteArray);
            obj.fromByteArray(byteArray);

            if(lapide == ' ' && obj.getID() == _id)
                return obj;

            arquivo.skipBytes(arquivo.readInt()); // Pular o lixo
        }

        return null;
    }
    
    /**
     * Exclui um registro
     * @param _id ID do registro
     * @return Se excluiu
     * @throws Exception
     */
    public boolean excluir(int _id) throws Exception
    {
        arquivo.seek(HEADER_SIZE);

        byte lapide;
        byte[] byteArray;
        int size;
        T obj = null;
        long endereco;

        while(arquivo.getFilePointer() < arquivo.length())
        {
            obj      = construtor.newInstance();
            endereco = arquivo.getFilePointer();
            lapide   = arquivo.readByte();
            size     = arquivo.readInt();

            byteArray = new byte[size];

            arquivo.read(byteArray);
            obj.fromByteArray(byteArray);

            if(lapide == ' ' && obj.getID() == _id)
            {
                arquivo.seek(endereco);
                arquivo.writeByte('*');

                return true;
            }

            arquivo.skipBytes(arquivo.readInt()); // Pular o lixo
        }

        return false;
    }

    /**
     * Recupera a posição de um registro
     * @param _id ID do registro
     * @return Posição do registro
     * @throws Exception
     */
    public long getPosicao(int _id) throws Exception
    {
        arquivo.seek(HEADER_SIZE);
        
        byte lapide;
        int size;
        int id;
        long endereco = -1;

        while(arquivo.getFilePointer() < arquivo.length())
        {
            endereco = arquivo.getFilePointer();
            lapide   = arquivo.readByte();
            size     = arquivo.readInt();
            id       = arquivo.readInt();

            if(lapide == ' ' && id == _id)
                return endereco;
            
            arquivo.seek(endereco + size + 5);
            arquivo.skipBytes(arquivo.readInt()); // Pular o lixo
        }

        return -1;
    }

    /**
     * Altera um registro
     * @param _obj Dados do registro
     * @return Se houve sucesso
     * @throws Exception
     */
    public boolean alterar(T _obj) throws Exception
    {
        if (_obj.getID() <= 0)
            return false;

        long endereco = getPosicao(_obj.getID());

        if (endereco <= 0)
            return false;

        byte[] objData = _obj.toByteArray();

        arquivo.seek(endereco);

        arquivo.skipBytes(1);

        int size = arquivo.readInt(); // Tamanho do registro

        arquivo.skipBytes(size);
        size += arquivo.readInt(); // O tamanho disponível é o tamanho do registro, mais o lixo após o registro

        arquivo.seek(endereco);

        if (size >= objData.length)
            this.incluir(_obj, size - objData.length, endereco);
        else
        {
            arquivo.writeByte('*');
            this.incluir(_obj);
        }

        return true;
    }
}
