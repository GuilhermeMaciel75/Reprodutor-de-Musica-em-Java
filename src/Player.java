import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.Collections;


public class Player {

    /**
     * The MPEG audio bitstream.
     */
    private Bitstream bitstream;
    /**
     * The MPEG audio decoder.
     */
    private Decoder decoder;
    /**
     * The AudioDevice where audio samples are written to.
     */
    private AudioDevice device;

    private PlayerWindow window;


    private boolean activeButtonPlayPause = true; //Variável booleana responsável por saber em qual estado o botão de play/ pause está
    private boolean pressButtonPlayPause = true; // Variável que é responsável por verificar se o botão de pause está ativo
    private boolean playing = false; //Variável que verificar se a música está sendo reproduzida
    private boolean doubleMusic = false; //Variável utilizada para verificar se duas músicas estão tocando ao mesmo tempo
    private boolean musicRunning = true; //Variável interna de quando a musica está sendo tocada
    private boolean activeLoop = false; // Variável responsável por verificar se o botão de loop está ativo
    private boolean activeShuffle = false; //Variável que verifica se o shuffle está ativo
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition lockCondition = lock.newCondition();


    //Array dinamicos, o primeiro salva todas as músicas da classe Song
    //O segundo armazena a lista inicial, antes do shuffle
    //Já o terceiro guarda apenas as musicas que serão coladas no display para vizualização pelo usuário
    private ArrayList<Song> songsListDynamic = new ArrayList<>();
    private ArrayList<Song> songsListDynamicBackup = new ArrayList<>(); //Array de Backup da lista antes do shuffle
    private ArrayList<String[]> musicsListDynamic = new ArrayList<String[]>();
    private String[][] musicsListStatic = {};

    //Variaveis utilizadas nas funções
    private Song songPlaying; // Variável do tipo song, que armazena as informações da música que está sendo tocada no momento
    private Song removeMusic; // Variável do do tipo song que armazena as informaçõs da música que foi removida da lista de reprodução

    private Song music;
    private int idxMusic; // Variável quer armazena o ínidice atual da música que esta sendo tocada ou Da música que será removida

    private int idxMusicRemove; //Variável que armazena o indice da música removida
    private int actualTime; //Variável utilizada para calcular o tempo que será pulado no scrubble

    private int loopi = 0; //Variável para indexar o array no for

    public int currentFrame = 0;

    //Função responsável por reproduzir a múscia
    //Ela antes de iniciar a reprodução, pega o indice da música selecionada pelo usuário
    private final ActionListener buttonListenerPlayNow = e -> {
        idxMusic = window.getIndex();
        musicRunner(idxMusic);
    };

    //Função responsável por remover o som selecionado da lista
    private final ActionListener buttonListenerRemove = e -> {
        Thread removeSong = new Thread(() -> {
            try{
                lock.lock();
                //Pegando a música que foi selecionada pelo usuário
                idxMusicRemove = window.getIndex();
                removeMusic = songsListDynamic.get(idxMusicRemove);

                //Removendo a música da lista mostrada ao usuário
                musicsListDynamic.remove(idxMusicRemove);
                musicsListStatic = musicsListDynamic.toArray(new String[this.musicsListDynamic.size()][7]);
                window.setQueueList(musicsListStatic);

                //Removendo a música da lista de songs
                removeMusic = songsListDynamic.remove(idxMusicRemove);
                songsListDynamicBackup.remove(removeMusic);

                if (idxMusic > idxMusicRemove){
                    idxMusic --;
                }

                else if(idxMusic == idxMusicRemove){
                    stop();
                }

                if(songsListDynamic.size() >= 2){
                    window.setEnabledShuffleButton(true);
                }
                else{
                    window.setEnabledShuffleButton(false);
                }

                if(songsListDynamic.size() >= 1){
                    window.setEnabledLoopButton(true);
                }
                else{
                    window.setEnabledLoopButton(false);
                }

            } finally {
                lock.unlock();
            }

        });

        removeSong.start();

    };
    //Função responsável por adicionar um novo som a lista, ele abre a janela de adicionar a música
    //Em uma nova thread
    private final ActionListener buttonListenerAddSong = e -> {
        Thread addSong = new Thread(() -> {
            Song music;

            //Abertura da aba para recebimento do arquivo de música do usuário
            try {
                lock.lock();
                music = this.window.openFileChooser();
                //Adicionando a nova música ao array de display
                musicsListDynamic.add(music.getDisplayInfo());
                musicsListStatic = musicsListDynamic.toArray(new String[this.musicsListDynamic.size()][7]);
                window.setQueueList(musicsListStatic);

                //Adicionando a nova música ao array de song (Array, que efetivamente executa a música)
                songsListDynamic.add(music);
                songsListDynamicBackup.add(music);

                if(songsListDynamic.size() >= 2){
                    window.setEnabledShuffleButton(true);
                }
                else{
                    window.setEnabledShuffleButton(false);
                }

                if(songsListDynamic.size() >= 1){
                    window.setEnabledLoopButton(true);
                }
                else{
                    window.setEnabledLoopButton(false);
                }

            } catch (IOException | BitstreamException | UnsupportedTagException | InvalidDataException ex) {
                throw new RuntimeException(ex);
            }finally {
                lock.unlock();
            }

        });

        addSong.start();

    };
    //Função resposável pelo botão Play/Pause, a medida em que clicamos nele,
    //Mudamos o seu estado e a continuação ou Pausa da música
    private final ActionListener buttonListenerPlayPause = e -> {

        //Caso o botão pause esteja ativo (Música Em execução)
        if (activeButtonPlayPause == true){
            pressButtonPlayPause = false;
            activeButtonPlayPause = false;
            window.setPlayPauseButtonIcon(0);
        }

        //Caso o botão esteja no estado de play (Música pausada)
        else{
            pressButtonPlayPause = true;
            activeButtonPlayPause = true;
            musicPlaying();
            window.setPlayPauseButtonIcon(1);
        }
    };
    //Função responsável por dar um STOP na música
    private final ActionListener buttonListenerStop = e -> {
        stop();
    };
    //Função responsável por pular para a proima música
    private final ActionListener buttonListenerNext = e -> {
        nextSong();
    };
    //Função responsável por retornar para a música anterior
    private final ActionListener buttonListenerPrevious = e -> {
        previousSong();
    };
    //Função responsável por realizar op shuffle da lista de música
    private final ActionListener buttonListenerShuffle = e -> {
        //Criando a Thread que será responsável por rodar os processos do shuffle
        Thread shuffle = new Thread(() -> {

            try {
                lock.lock();
                //Estrutura de condicional que verifica o estado do botão shuffle
                if(!activeShuffle) {
                    activeShuffle = true; //Muda o estado atual
                    songsListDynamicBackup = (ArrayList<Song>) songsListDynamic.clone(); //Cria um clone da lista inical
                    music = songsListDynamicBackup.get(idxMusic); // Pega a múscia que está sendo tocada atualmente
                    Collections.shuffle(songsListDynamic); // Embaralha da lista de músicas
                    songsListDynamic.remove(music); // Remove a música que está sendo tocada da lista
                    songsListDynamic.add(0, music); //Adiciona novamente essa música no ínicio da lista embaralhada
                    idxMusic = 0;

                } else {
                    activeShuffle = false; //Muda o estado atual
                    music = songsListDynamic.get(idxMusic); // Pega a múscia que está sendo tocada atualmente
                    songsListDynamic = songsListDynamicBackup; //Pega o backup feito da lista inicial
                    idxMusic = songsListDynamic.indexOf(music); //Muda o valor do indice da música atual para o mesmo indice na lista inical

                }
                musicsListDynamic.clear(); //Limpando a lista dinâmica de musicas (Aquela que é usada para mostrar no palyer)
                for (Song i : songsListDynamic){
                    musicsListDynamic.add(i.getDisplayInfo()); //Adciona os metadados na música a essa lista de string a essa nova lista
                    loopi ++;
                }

                //Atualiza o valor no player
                musicsListStatic = musicsListDynamic.toArray(new String[this.musicsListDynamic.size()][7]);
                window.setQueueList(musicsListStatic);
            } finally {
                lock.unlock();
            }

        });

        shuffle.start();


    };
    private final ActionListener buttonListenerLoop = e -> {
        activeLoop = !activeLoop; //Muda o estado do Loop

    };
    //Função responsável por mudar o tempo da música a partir do clique no scruber
    private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            jumpSong();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            press();
        }

        @Override
        public void mouseDragged(MouseEvent e) {

        }
    };

    public Player() {
        EventQueue.invokeLater(() -> window = new PlayerWindow(
                ("Spotfy"),
                musicsListStatic,
                buttonListenerPlayNow,
                buttonListenerRemove,
                buttonListenerAddSong,
                buttonListenerShuffle,
                buttonListenerPrevious,
                buttonListenerPlayPause,
                buttonListenerStop,
                buttonListenerNext,
                buttonListenerLoop,
                scrubberMouseInputAdapter)
        );
    }


    //<editor-fold desc="Essential">

    /**
     * @return False if there are no more frames to play.
     */
    private boolean playNextFrame() throws JavaLayerException {
        // TODO Is this thread safe?
        if (device != null) {
            Header h = bitstream.readFrame();
            if (h == null) return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            device.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();
            currentFrame++;
        }
        return true;
    }

    /**
     * @return False if there are no more frames to skip.
     */
    private boolean skipNextFrame() throws BitstreamException {
        // TODO Is this thread safe?
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        currentFrame++;
        return true;
    }

    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     * @throws BitstreamException Generic Bitstream exception.
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        // TODO Is this thread safe?
        if (newFrame > currentFrame) {
            int framesToSkip = newFrame - currentFrame;
            boolean condition = true;
            while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
        }
    }

    /**
     * Função responsável por pausar a reprodução da música e zerar o MiniPlayer
     *
     *
     */
    private void stop(){
        playing = false;
        pressButtonPlayPause = false;
        window.resetMiniPlayer();

    }

    /**
     * Função responsável por efetivamente rodar a música e atualizar as informações no player
     *
     */
    public void musicPlaying(){

        Thread running = new Thread(() -> {
            musicRunning = true;
            // Loop que irá reproduzir a música até ela finalizar ou algum evento de Pause / Quit for acionado
            while(musicRunning && pressButtonPlayPause) {
                try {
                    // Condicional que verifica se duas músicas estão tocando ao mesmo tempo, e finaliza a execução da primeira
                    if (doubleMusic) {
                        doubleMusic = false;
                        break;
                    }
                    //Atualozando valores do Player
                    window.setTime((int) (currentFrame * (int) songPlaying.getMsPerFrame()), (int) songPlaying.getMsLength());

                    //Estrutura condiocional que reproduz a música, caso ainda exista música a ser reproduzida
                    if (window.getScrubberValue() < songPlaying.getMsLength()) {

                        musicRunning = playNextFrame();

                    } else {

                        musicRunning = false;
                    }

                    //Verifica se o botão de STOP foi pressionado, caso tenha sido, sai do loop e finaliza a execução da Thread
                    if(!pressButtonPlayPause) {
                        break;
                    }

                    //Estrutura de condicional que ativa ou desativa os botões de previous e next
                    if(songsListDynamic.size() == 1){
                        window.setEnabledNextButton(false);
                        window.setEnabledPreviousButton(false);
                    }

                    else if(songsListDynamic.size() > 1 && idxMusic == 0) {
                        window.setEnabledNextButton(true);
                        window.setEnabledPreviousButton(false);

                    }

                    else if(songsListDynamic.size() > idxMusic + 1 && idxMusic > 0){
                        window.setEnabledNextButton(true);
                        window.setEnabledPreviousButton(true);
                    }

                    else if(idxMusic == songsListDynamic.size() - 1){
                        window.setEnabledNextButton(false);
                        window.setEnabledPreviousButton(true);
                    }

                } catch (JavaLayerException e) {
                    throw new RuntimeException(e);
                }
            }

            // Estrutura condiconal que verifica o que se faz ao término de uma música, caso não haja mais nenhuma música, ou caso ele tenha
            // Que passar para a execução da próxima música da lista
            if(!musicRunning){
                playing = false;
            }

            if(playing == false && pressButtonPlayPause == true && idxMusic + 1 < musicsListDynamic.size()){
                nextSong();
            }

            //Caso tenha chegado ao fim da lista de reprodução e o botão de loop esteja ativo, reinicia a reprodução
            else if(playing == false && activeLoop == true && idxMusic + 1 == musicsListDynamic.size()){
                idxMusic = 0;
                musicRunner(idxMusic);
            }

            else if(idxMusic + 1 > musicsListDynamic.size()) {
                stop();
            }


        });

        running.start();
    }
    /**
     * Função responsável por atualizar as informações do miniplayer e criar os dipositivos de audio
     *
     */
    public void musicRunner(int idxMusic){
        currentFrame = 0;

        //Verifica se há duas músicas tocando e muda o valor da variável doubleMusic afim de parar a execução dessa música
        if(playing){
            doubleMusic = true;
        }

        //Criação da Thread de execução de uma música
        Thread playingThread = new Thread(()->{
            try{
                lock.lock();
                playing = true;

                //Pegando a música especificada pelo usuário (Aquela que ele clicou)
                songPlaying = songsListDynamic.get(idxMusic);

                //Realizando Update no Player de múscia e setando alguns botões
                window.setPlayingSongInfo(songPlaying.getTitle(), songPlaying.getAlbum(), songPlaying.getArtist()); //Setando as informações na tela

                if(pressButtonPlayPause){
                    window.setPlayPauseButtonIcon(1);
                }
                else{
                    window.setPlayPauseButtonIcon(0);
                }

                window.setEnabledPlayPauseButton(playing);
                window.setEnabledStopButton(playing);
                window.setEnabledScrubber(playing);

                window.setEnabledLoopButton(playing);

                try {
                    //Criando o dispositivo de áudio e inicializando a reprodução da múxica
                    device = FactoryRegistry.systemRegistry().createAudioDevice();
                    device.open(decoder = new Decoder());
                    bitstream = new Bitstream(songPlaying.getBufferedInputStream());
                    musicPlaying();

                } catch (JavaLayerException | FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }

            }
            finally {
                lock.unlock();
            }

        });

        playingThread.start();
    }

    //Função responsável por passar para a próxima música, atualizando o valor do idxMusic e iniciando a execução dela
    public void nextSong(){
        if(idxMusic + 1 < musicsListDynamic.size()){
            //Caso a música esteja pauasada, ele a finaliza e muda o estado do botão
            if(!activeButtonPlayPause){
                stop();
                pressButtonPlayPause = true;
                activeButtonPlayPause = true;
                window.setPlayPauseButtonIcon(1);
            }

            idxMusic ++;
            musicRunner(idxMusic);
        }
    }

    //Função responsável por retornar para a música anterior, atualizando o valor do idxMusic e iniciando a execução dela
    public  void previousSong(){
        if(idxMusic - 1 >= 0){
            //Caso a música esteja pauasada, ele a finaliza e muda o estado do botão
            if(!activeButtonPlayPause) {
                stop();
                pressButtonPlayPause = true;
                activeButtonPlayPause = true;
                window.setPlayPauseButtonIcon(1);
            }
            idxMusic --;
            musicRunner(idxMusic);

        }
    }

    //Função que Pula ou retrocede a música conforme o alterado no Scrubber
    public void jumpSong(){
        try {
            //Recriando o device, decoder e bitstream, para possibilitar
            //Voltar para um ponto da música

            currentFrame = 0;
            device = FactoryRegistry.systemRegistry().createAudioDevice();
            device.open(decoder = new Decoder());
            bitstream = new Bitstream(songPlaying.getBufferedInputStream());

        } catch (JavaLayerException | FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }

        //Pegando o valor atual do scruber, e atualizando o MiniPlayer
        actualTime = (int) (window.getScrubberValue() / songPlaying.getMsPerFrame());
        window.setTime((int) (actualTime * (int) songPlaying.getMsPerFrame()), (int) songPlaying.getMsLength());

        //Pulando para os Bits que foram "Escolhidos" ao alterar o Scrubber
        try {
            skipToFrame(actualTime);
        } catch (BitstreamException e) {
            throw new RuntimeException(e);
        }

        if(playing && activeButtonPlayPause == true){
            pressButtonPlayPause = true;
        }
        musicPlaying();

    }

    //Função responsável por pausar a reprodução da música quando o scruber for segurado e arrastado
    public void press(){
        pressButtonPlayPause = false;

    }
    //</editor-fold>
}